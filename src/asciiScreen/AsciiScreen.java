package asciiscreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.FloatFrameBuffer;
import com.badlogic.gdx.utils.viewport.*;
import java.nio.ByteBuffer;
// import java.awt.Color;
// import java.awt.Dimension;
// import java.awt.Graphics;
// import java.awt.Image;
// import java.awt.image.BufferedImage;
// import java.awt.image.LookupOp;
// import java.awt.image.ShortLookupTable;
// import java.io.IOException;
// import javax.imageio.ImageIO;
// import javax.swing.JPanel;

/**
 * This simulates a code page 437 ASCII terminal display.
 * @author Trystan Spangler
 */
public class AsciiScreen extends ScreenAdapter {
	private static final long serialVersionUID = 1;

    /**
     * The color black (pure black).
     */
    public static Color black = new Color(0, 0, 0, 1);

    /**
     * The color red.
     */
    public static Color red = new Color(0.5f, 0, 0, 1);

    /**
     * The color green.
     */
    public static Color green = new Color(0, 0.5f, 0, 1);

    /**
     * The color yellow.
     */
    public static Color yellow = new Color(0.5f, 0.5f, 0, 1);

    /**
     * The color blue.
     */
    public static Color blue = new Color(0, 0, 0.5f, 1);

    /**
     * The color magenta.
     */
    public static Color magenta = new Color(0.5f, 0, 0.5f, 1);

    /**
     * The color cyan.
     */
    public static Color cyan = new Color(0, 0.5f, 0.5f, 1);

    /**
     * The color white (light gray).
     */
    public static Color white = new Color(0.75f, 0.75f, 0.75f, 1);

    /**
     * A brighter black (dark gray).
     */
    public static Color brightBlack = new Color(0.5f, 0.5f, 0.5f, 1);

    /**
     * A brighter red.
     */
    public static Color brightRed = new Color(1, 0, 0, 1);

    /**
     * A brighter green.
     */
    public static Color brightGreen = new Color(0, 1, 0, 1);

    /**
     * A brighter yellow.
     */
    public static Color brightYellow = new Color(1, 1, 0, 1);

    /**
     * A brighter blue.
     */
    public static Color brightBlue = new Color(0, 0, 1, 1);

    /**
     * A brighter magenta.
     */
    public static Color brightMagenta = new Color(1, 0, 1, 1);

    /**
     * A brighter cyan.
     */
    public static Color brightCyan = new Color(0, 1, 1, 1);

    /**
     * A brighter white (pure white).
     */
    public static Color brightWhite = new Color(1, 1, 1, 1);

	/* Original fields */
    // private Image offscreenBuffer;
    // private Graphics offscreenGraphics;
    private int widthInCharacters;
    private int heightInCharacters;
    private int charWidth = 9;
    private int charHeight = 16;
    private Color defaultBackgroundColor;
    private Color defaultForegroundColor;
    private int cursorX;
    private int cursorY;
    // private BufferedImage glyphSprite; // replaced by Texture
    // private BufferedImage[] glyphs; // replaced by TextureRegion
    private char[][] chars;
    private Color[][] backgroundColors;
    private Color[][] foregroundColors;
    private char[][] oldChars;
    private Color[][] oldBackgroundColors;
    private Color[][] oldForegroundColors;

	/* Ported fields */
	private Viewport viewport;
	private Camera camera;
	private Texture glyphSprite;
	private TextureRegion[] glyphs;
	private SpriteBatch batch;
	// for rendering background
	private Texture backgroundTexture;
	private FloatFrameBuffer frameBuffer;
	private TextureRegion frameRegion;
	/**
	* Class constructor.
	* Default size is 80x24.
	*/
	public AsciiScreen() {
		this(80, 24);
	}

	/**
	* Class constructor specifying the width and height in characters.
	* @param width
	* @param height
	*/
	public AsciiScreen(int width, int height) {
		super();

		if (width < 1){
			throw new IllegalArgumentException("width " + width + " must be greater than 0." );
		}

		if (height < 1){
			throw new IllegalArgumentException("height " + height + " must be greater than 0." );
		}

		widthInCharacters = width;
		heightInCharacters = height;

		this.camera = new OrthographicCamera();
		this.viewport = new FitViewport(widthInCharacters * charWidth, heightInCharacters * charHeight, this.camera);
		this.batch = new SpriteBatch();

		Pixmap whitebackground = new Pixmap(charWidth, charHeight, Pixmap.Format.RGBA8888);
		whitebackground.setColor(white);
		whitebackground.fill();
		this.backgroundTexture = new Texture(whitebackground);
		whitebackground.dispose();

		defaultBackgroundColor = black;
		defaultForegroundColor = white;

		chars = new char[widthInCharacters][heightInCharacters];
		backgroundColors = new Color[widthInCharacters][heightInCharacters];
		foregroundColors = new Color[widthInCharacters][heightInCharacters];

		oldChars = new char[widthInCharacters][heightInCharacters];
		oldBackgroundColors = new Color[widthInCharacters][heightInCharacters];
		oldForegroundColors = new Color[widthInCharacters][heightInCharacters];

		glyphs = new TextureRegion[256];

		loadGlyphs();

		AsciiScreen.this.clear();
	}

	public void resize(int width, int height) {
		this.viewport.update(width, height, true);
	}

    @Override
    public void render(float delta) {
		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        paint(delta);
		batch.setColor(white);
		batch.begin();
		batch.draw(frameRegion, 0, 0);
		batch.end();
    }

	public void paint(float delta){
		if(frameBuffer == null){
			frameBuffer = new FloatFrameBuffer(charWidth * widthInCharacters, charHeight * heightInCharacters, false);
			frameBuffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		}
		if(frameRegion == null){
			frameRegion = new TextureRegion(frameBuffer.getColorBufferTexture());
			frameRegion.flip(false, true);
		}
		frameBuffer.begin();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		for (int x = 0; x < widthInCharacters; x++) {
			for (int y = 0; y < heightInCharacters; y++) {
				if (oldBackgroundColors[x][y] == backgroundColors[x][y]
				&& oldForegroundColors[x][y] == foregroundColors[x][y]
				&& oldChars[x][y] == chars[x][y]){
					continue;
				}
				Color bg = backgroundColors[x][y];
				Color fg = foregroundColors[x][y];

				batch.setColor(bg);
				batch.draw(backgroundTexture, x * charWidth, (heightInCharacters - y - 1) * charHeight, charWidth, charHeight);
				batch.setColor(fg);
				batch.draw(glyphs[chars[x][y]], x * charWidth, (heightInCharacters - y - 1) * charHeight, charWidth, charHeight);

				oldBackgroundColors[x][y] = backgroundColors[x][y];
				oldForegroundColors[x][y] = foregroundColors[x][y];
				oldChars[x][y] = chars[x][y];
			}
		}
		batch.end();
		frameBuffer.end();
	}

    private void loadGlyphs() {
		// replace the string with the file's actually path (relative to working directory, which in default is "assets/").
		Pixmap pixmap = new Pixmap(Gdx.files.internal("texture/cp437.png"));
		Pixmap pixmapDst = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA8888);
		// transfer the image into white-alpha
		ByteBuffer buffer = pixmap.getPixels();
		ByteBuffer bufferDst = pixmapDst.getPixels();
		buffer.rewind();
		bufferDst.rewind();
		System.out.println(buffer.limit());
		System.out.println(bufferDst.limit());
		while(buffer.hasRemaining()){
			byte a = buffer.get();
			byte r = 0;
			byte g = 0;
			byte b = 0;
			if(a != 0){
				r = (byte)127;
				g = (byte)127;
				b = (byte)127;
			} //@TODO
			bufferDst.put(r);
			bufferDst.put(g);
			bufferDst.put(b);
			bufferDst.put(a);
		}
		buffer.rewind();
		bufferDst.rewind();

        glyphSprite = new Texture(pixmapDst);
		glyphSprite.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        for (int i = 0; i < 256; i++) {
            int startX = (i % 32) * charWidth + 7; // 8 is the padding, same below.
            int startY = (i / 32) * charHeight + 7;

            glyphs[i] = new TextureRegion(glyphSprite, startX, startY, charWidth, charHeight);
        }
		pixmap.dispose();
    }

    /**
     * Gets the height, in pixels, of a character.
     * @return
     */
    public int getCharHeight() {
        return charHeight;
    }

    /**
     * Gets the width, in pixels, of a character.
     * @return
     */
    public int getCharWidth() {
        return charWidth;
    }

    /**
     * Gets the height in characters.
     * A standard terminal is 24 characters high.
     * @return
     */
    public int getHeightInCharacters() {
        return heightInCharacters;
    }

    /**
     * Gets the width in characters.
     * A standard terminal is 80 characters wide.
     * @return
     */
    public int getWidthInCharacters() {
        return widthInCharacters;
    }

    /**
     * Gets the distance from the left new text will be written to.
     * @return
     */
    public int getCursorX() {
        return cursorX;
    }

    /**
     * Sets the distance from the left new text will be written to.
     * This should be equal to or greater than 0 and less than the the width in characters.
     * @param cursorX the distance from the left new text should be written to
     */
    public void setCursorX(int cursorX) {
        if (cursorX < 0 || cursorX >= widthInCharacters)
            throw new IllegalArgumentException("cursorX " + cursorX + " must be within range [0," + widthInCharacters + ")." );

        this.cursorX = cursorX;
    }

    /**
     * Gets the distance from the top new text will be written to.
     * @return
     */
    public int getCursorY() {
        return cursorY;
    }

    /**
     * Sets the distance from the top new text will be written to.
     * This should be equal to or greater than 0 and less than the the height in characters.
     * @param cursorY the distance from the top new text should be written to
     */
    public void setCursorY(int cursorY) {
        if (cursorY < 0 || cursorY >= heightInCharacters)
            throw new IllegalArgumentException("cursorY " + cursorY + " must be within range [0," + heightInCharacters + ")." );

        this.cursorY = cursorY;
    }

    /**
     * Sets the x and y position of where new text will be written to. The origin (0,0) is the upper left corner.
     * The x should be equal to or greater than 0 and less than the the width in characters.
     * The y should be equal to or greater than 0 and less than the the height in characters.
     * @param x the distance from the left new text should be written to
     * @param y the distance from the top new text should be written to
     */
    public void setCursorPosition(int x, int y) {
        setCursorX(x);
        setCursorY(y);
    }

    /**
     * Gets the default background color that is used when writing new text.
     * @return
     */
    public Color getDefaultBackgroundColor() {
        return defaultBackgroundColor;
    }

    /**
     * Sets the default background color that is used when writing new text.
     * @param defaultBackgroundColor
     */
    public void setDefaultBackgroundColor(Color defaultBackgroundColor) {
        if (defaultBackgroundColor == null){
            // throw new NullPointerException("defaultBackgroundColor must not be null.");
			this.defaultBackgroundColor = black;
		}else{
        	this.defaultBackgroundColor = defaultBackgroundColor;
		}
    }

    /**
     * Gets the default foreground color that is used when writing new text.
     * @return
     */
    public Color getDefaultForegroundColor() {
        return defaultForegroundColor;
    }

    /**
     * Sets the default foreground color that is used when writing new text.
     * @param defaultForegroundColor
     */
    public void setDefaultForegroundColor(Color defaultForegroundColor) {
        if (defaultForegroundColor == null){
			this.defaultForegroundColor = white;
		}else{
			this.defaultForegroundColor = defaultForegroundColor;
		}
    }

    /**
     * Clear the entire screen to whatever the default background color is.
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen clear() {
        return clear(' ', 0, 0, widthInCharacters, heightInCharacters, defaultForegroundColor, defaultBackgroundColor);
    }

    /**
     * Clear the entire screen with the specified character and whatever the default foreground and background colors are.
     * @param character  the character to write
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen clear(char character) {
        if (character < 0 || character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "]." );

        return clear(character, 0, 0, widthInCharacters, heightInCharacters, defaultForegroundColor, defaultBackgroundColor);
    }

    /**
     * Clear the entire screen with the specified character and whatever the specified foreground and background colors are.
     * @param character  the character to write
     * @param foreground the foreground color or null to use the default
     * @param background the background color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen clear(char character, Color foreground, Color background) {
        if (character < 0 || character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "]." );

        return clear(character, 0, 0, widthInCharacters, heightInCharacters, foreground, background);
    }

    /**
     * Clear the section of the screen with the specified character and whatever the default foreground and background colors are.
     * @param character  the character to write
     * @param x          the distance from the left to begin writing from
     * @param y          the distance from the top to begin writing from
     * @param width      the height of the section to clear
     * @param height     the width of the section to clear
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen clear(char character, int x, int y, int width, int height) {
        if (character < 0 || character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "]." );

        if (x < 0 || x >= widthInCharacters)
            throw new IllegalArgumentException("x " + x + " must be within range [0," + widthInCharacters + ")." );

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")." );

        if (width < 1)
            throw new IllegalArgumentException("width " + width + " must be greater than 0." );

        if (height < 1)
            throw new IllegalArgumentException("height " + height + " must be greater than 0." );

        if (x + width > widthInCharacters)
            throw new IllegalArgumentException("x + width " + (x + width) + " must be less than " + (widthInCharacters + 1) + "." );

        if (y + height > heightInCharacters)
            throw new IllegalArgumentException("y + height " + (y + height) + " must be less than " + (heightInCharacters + 1) + "." );


        return clear(character, x, y, width, height, defaultForegroundColor, defaultBackgroundColor);
    }

    /**
     * Clear the section of the screen with the specified character and whatever the specified foreground and background colors are.
     * @param character  the character to write
     * @param x          the distance from the left to begin writing from
     * @param y          the distance from the top to begin writing from
     * @param width      the height of the section to clear
     * @param height     the width of the section to clear
     * @param foreground the foreground color or null to use the default
     * @param background the background color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen clear(char character, int x, int y, int width, int height, Color foreground, Color background) {
        if (character < 0 || character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "]." );

        if (x < 0 || x >= widthInCharacters)
            throw new IllegalArgumentException("x " + x + " must be within range [0," + widthInCharacters + ")" );

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")" );

        if (width < 1)
            throw new IllegalArgumentException("width " + width + " must be greater than 0." );

        if (height < 1)
            throw new IllegalArgumentException("height " + height + " must be greater than 0." );

        if (x + width > widthInCharacters)
            throw new IllegalArgumentException("x + width " + (x + width) + " must be less than " + (widthInCharacters + 1) + "." );

        if (y + height > heightInCharacters)
            throw new IllegalArgumentException("y + height " + (y + height) + " must be less than " + (heightInCharacters + 1) + "." );

        for (int xo = x; xo < x + width; xo++) {
            for (int yo = y; yo < y + height; yo++) {
                write(character, xo, yo, foreground, background);
            }
        }
        return this;
    }

    /**
     * Write a character to the cursor's position.
     * This updates the cursor's position.
     * @param character  the character to write
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(char character) {
        if (character < 0 || character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "]." );

        return write(character, cursorX, cursorY, defaultForegroundColor, defaultBackgroundColor);
    }

    /**
     * Write a character to the cursor's position with the specified foreground color.
     * This updates the cursor's position but not the default foreground color.
     * @param character  the character to write
     * @param foreground the foreground color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(char character, Color foreground) {
        if (character < 0 || character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "]." );

        return write(character, cursorX, cursorY, foreground, defaultBackgroundColor);
    }

    /**
     * Write a character to the cursor's position with the specified foreground and background colors.
     * This updates the cursor's position but not the default foreground or background colors.
     * @param character  the character to write
     * @param foreground the foreground color or null to use the default
     * @param background the background color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(char character, Color foreground, Color background) {
        if (character < 0 || character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "]." );

        return write(character, cursorX, cursorY, foreground, background);
    }

    /**
     * Write a character to the specified position.
     * This updates the cursor's position.
     * @param character  the character to write
     * @param x          the distance from the left to begin writing from
     * @param y          the distance from the top to begin writing from
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(char character, int x, int y) {
        if (character < 0 || character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "]." );

        if (x < 0 || x >= widthInCharacters)
            throw new IllegalArgumentException("x " + x + " must be within range [0," + widthInCharacters + ")" );

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")" );

        return write(character, x, y, defaultForegroundColor, defaultBackgroundColor);
    }

    /**
     * Write a character to the specified position with the specified foreground color.
     * This updates the cursor's position but not the default foreground color.
     * @param character  the character to write
     * @param x          the distance from the left to begin writing from
     * @param y          the distance from the top to begin writing from
     * @param foreground the foreground color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(char character, int x, int y, Color foreground) {
        if (character < 0 || character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "]." );

        if (x < 0 || x >= widthInCharacters)
            throw new IllegalArgumentException("x " + x + " must be within range [0," + widthInCharacters + ")" );

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")" );

        return write(character, x, y, foreground, defaultBackgroundColor);
    }

    /**
     * Write a character to the specified position with the specified foreground and background colors.
     * This updates the cursor's position but not the default foreground or background colors.
     * @param character  the character to write
     * @param x          the distance from the left to begin writing from
     * @param y          the distance from the top to begin writing from
     * @param foreground the foreground color or null to use the default
     * @param background the background color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(char character, int x, int y, Color foreground, Color background) {
        if (character < 0 || character >= glyphs.length)
            throw new IllegalArgumentException("character " + character + " must be within range [0," + glyphs.length + "]." );

        if (x < 0 || x >= widthInCharacters)
            throw new IllegalArgumentException("x " + x + " must be within range [0," + widthInCharacters + ")" );

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")" );

        if (foreground == null) foreground = defaultForegroundColor;
        if (background == null) background = defaultBackgroundColor;

        chars[x][y] = character;
        foregroundColors[x][y] = foreground;
        backgroundColors[x][y] = background;
        cursorX = x + 1;
        cursorY = y;
        return this;
    }

    /**
     * Write a string to the cursor's position.
     * This updates the cursor's position.
     * @param string     the string to write
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(String string) {
        if (string == null)
            throw new NullPointerException("string must not be null" );

        if (cursorX + string.length() >= widthInCharacters)
            throw new IllegalArgumentException("cursorX + string.length() " + (cursorX + string.length()) + " must be less than " + widthInCharacters + "." );

        return write(string, cursorX, cursorY, defaultForegroundColor, defaultBackgroundColor);
    }

    /**
     * Write a string to the cursor's position with the specified foreground color.
     * This updates the cursor's position but not the default foreground color.
     * @param string     the string to write
     * @param foreground the foreground color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(String string, Color foreground) {
        if (string == null)
            throw new NullPointerException("string must not be null" );

        if (cursorX + string.length() >= widthInCharacters)
            throw new IllegalArgumentException("cursorX + string.length() " + (cursorX + string.length()) + " must be less than " + widthInCharacters + "." );

        return write(string, cursorX, cursorY, foreground, defaultBackgroundColor);
    }

    /**
     * Write a string to the cursor's position with the specified foreground and background colors.
     * This updates the cursor's position but not the default foreground or background colors.
     * @param string     the string to write
     * @param foreground the foreground color or null to use the default
     * @param background the background color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(String string, Color foreground, Color background) {
        if (string == null)
            throw new NullPointerException("string must not be null" );

        if (cursorX + string.length() >= widthInCharacters)
            throw new IllegalArgumentException("cursorX + string.length() " + (cursorX + string.length()) + " must be less than " + widthInCharacters + "." );

        return write(string, cursorX, cursorY, foreground, background);
    }

    /**
     * Write a string to the specified position.
     * This updates the cursor's position.
     * @param string     the string to write
     * @param x          the distance from the left to begin writing from
     * @param y          the distance from the top to begin writing from
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(String string, int x, int y) {
        if (string == null)
            throw new NullPointerException("string must not be null" );

        if (x + string.length() >= widthInCharacters)
            throw new IllegalArgumentException("x + string.length() " + (x + string.length()) + " must be less than " + widthInCharacters + "." );

        if (x < 0 || x >= widthInCharacters)
            throw new IllegalArgumentException("x " + x + " must be within range [0," + widthInCharacters + ")" );

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")" );

        return write(string, x, y, defaultForegroundColor, defaultBackgroundColor);
    }

    /**
     * Write a string to the specified position with the specified foreground color.
     * This updates the cursor's position but not the default foreground color.
     * @param string     the string to write
     * @param x          the distance from the left to begin writing from
     * @param y          the distance from the top to begin writing from
     * @param foreground the foreground color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(String string, int x, int y, Color foreground) {
        if (string == null)
            throw new NullPointerException("string must not be null" );

        if (x + string.length() >= widthInCharacters)
            throw new IllegalArgumentException("x + string.length() " + (x + string.length()) + " must be less than " + widthInCharacters + "." );

        if (x < 0 || x >= widthInCharacters)
            throw new IllegalArgumentException("x " + x + " must be within range [0," + widthInCharacters + ")" );

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")" );

        return write(string, x, y, foreground, defaultBackgroundColor);
    }

    /**
     * Write a string to the specified position with the specified foreground and background colors.
     * This updates the cursor's position but not the default foreground or background colors.
     * @param string     the string to write
     * @param x          the distance from the left to begin writing from
     * @param y          the distance from the top to begin writing from
     * @param foreground the foreground color or null to use the default
     * @param background the background color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen write(String string, int x, int y, Color foreground, Color background) {
        if (string == null)
            throw new NullPointerException("string must not be null." );

        if (x + string.length() >= widthInCharacters)
            throw new IllegalArgumentException("x + string.length() " + (x + string.length()) + " must be less than " + widthInCharacters + "." );

        if (x < 0 || x >= widthInCharacters)
            throw new IllegalArgumentException("x " + x + " must be within range [0," + widthInCharacters + ")." );

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")." );

        if (foreground == null)
            foreground = defaultForegroundColor;

        if (background == null)
            background = defaultBackgroundColor;

        for (int i = 0; i < string.length(); i++) {
            write(string.charAt(i), x + i, y, foreground, background);
        }
        return this;
    }

    /**
     * Write a string to the center of the panel at the specified y position.
     * This updates the cursor's position.
     * @param string     the string to write
     * @param y          the distance from the top to begin writing from
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen writeCenter(String string, int y) {
        if (string == null)
            throw new NullPointerException("string must not be null" );

        if (string.length() >= widthInCharacters)
            throw new IllegalArgumentException("string.length() " + string.length() + " must be less than " + widthInCharacters + "." );

        int x = (widthInCharacters - string.length()) / 2;

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")" );

        return write(string, x, y, defaultForegroundColor, defaultBackgroundColor);
    }

    /**
     * Write a string to the center of the panel at the specified y position with the specified foreground color.
     * This updates the cursor's position but not the default foreground color.
     * @param string     the string to write
     * @param y          the distance from the top to begin writing from
     * @param foreground the foreground color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen writeCenter(String string, int y, Color foreground) {
        if (string == null)
            throw new NullPointerException("string must not be null" );

        if (string.length() >= widthInCharacters)
            throw new IllegalArgumentException("string.length() " + string.length() + " must be less than " + widthInCharacters + "." );

        int x = (widthInCharacters - string.length()) / 2;

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")" );

        return write(string, x, y, foreground, defaultBackgroundColor);
    }

    /**
     * Write a string to the center of the panel at the specified y position with the specified foreground and background colors.
     * This updates the cursor's position but not the default foreground or background colors.
     * @param string     the string to write
     * @param y          the distance from the top to begin writing from
     * @param foreground the foreground color or null to use the default
     * @param background the background color or null to use the default
     * @return this for convenient chaining of method calls
     */
    public AsciiScreen writeCenter(String string, int y, Color foreground, Color background) {
        if (string == null)
            throw new NullPointerException("string must not be null." );

        if (string.length() >= widthInCharacters)
            throw new IllegalArgumentException("string.length() " + string.length() + " must be less than " + widthInCharacters + "." );

        int x = (widthInCharacters - string.length()) / 2;

        if (y < 0 || y >= heightInCharacters)
            throw new IllegalArgumentException("y " + y + " must be within range [0," + heightInCharacters + ")." );

        if (foreground == null)
            foreground = defaultForegroundColor;

        if (background == null)
            background = defaultBackgroundColor;

        for (int i = 0; i < string.length(); i++) {
            write(string.charAt(i), x + i, y, foreground, background);
        }
        return this;
    }

    public void withEachTile(TileTransformer transformer){
		withEachTile(0, 0, widthInCharacters, heightInCharacters, transformer);
    }

    public void withEachTile(int left, int top, int width, int height, TileTransformer transformer){
		AsciiCharacterData data = new AsciiCharacterData();

    	for (int x0 = 0; x0 < width; x0++)
    	for (int y0 = 0; y0 < height; y0++){
    		int x = left + x0;
    		int y = top + y0;

    		if (x < 0 || y < 0 || x >= widthInCharacters || y >= heightInCharacters)
    			continue;

    		data.character = chars[x][y];
    		data.foregroundColor = foregroundColors[x][y];
    		data.backgroundColor = backgroundColors[x][y];

    		transformer.transformTile(x, y, data);

    		chars[x][y] = data.character;
    		foregroundColors[x][y] = data.foregroundColor;
    		backgroundColors[x][y] = data.backgroundColor;
    	}
    }
}
