package gui;

import java.awt.*;
import java.awt.event.*;
import aos.jack.util.cursor.Action;
import aos.util.ThreadPool;

/**
   This is the main window.
**/
public class Towers extends Frame {

    public static void main(String [] arg)
    {
        new Towers("Testing").setVisible(true);
    }

    Drawable canvas;
    boolean showing;
    
    public Towers(String title) {
        super(title);
        add(canvas = new Drawable(this));

        addWindowListener( 
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit( 0 );
                }
            }
        );
        pack();
    }

    public void paint(Graphics g) {
        canvas.paint(g);
    }

    public void addDisc(int disc,int p) throws Exception {
        canvas.addDisc(disc,p);
    }

    public Action moveDisc(int disc,int to) throws Exception {
        return canvas.moveDisc(disc,to);
    }
}

class Drawable extends Canvas {

    Towers window;
    ThreadPool tp = new ThreadPool();

    /**
       Main window size.
    **/
    static int WIDTH = 300;
    static int HEIGHT = 200;

    /**
       The module width of the largest disc.
    **/
    static int LARGEST = 9;
    static int BORDER = 10;

    static int MODULE_WIDTH;
    static int MODULE_HEIGHT;
    static int VERTICAL_BASE;
    static int GAP;

    boolean resized = true;
    int dx = 0;
    int dy = 0;
    int [] center = new int[3];
    int PIN_HEIGHT;

    synchronized void setup()
    {
	Dimension d = getSize();
	if (d.width != WIDTH || d.height != HEIGHT) {
	    dx = d.width - WIDTH;
	    dy = d.height - HEIGHT;
	    WIDTH = d.width;
	    HEIGHT = d.height;
	    resized = true;
	}
 
	MODULE_WIDTH = (WIDTH - 2 * BORDER) / (6*(LARGEST+1)+6);
	MODULE_HEIGHT = (HEIGHT - 2 * BORDER) / (2 * LARGEST);
	VERTICAL_BASE = HEIGHT - (LARGEST + 4) * MODULE_HEIGHT;
	GAP = WIDTH - (6 * (LARGEST + 1) + 6 ) * MODULE_WIDTH;
	GAP /= 2;

	center[0] = offset(1) + GAP + MODULE_WIDTH;
	center[1] = center[0] + 2 * offset(1) + MODULE_WIDTH;
	center[2] = center[1] + 2 * offset(1) + MODULE_WIDTH;
	PIN_HEIGHT = (LARGEST+2) * MODULE_HEIGHT;
    }

    /**
       All things paintable.
    **/
    Block [] things;

    /**
       Adding to the paintables.
    **/
    void addThing(Block p)
    {
	int size = (things==null)? 0 : things.length;
	Block [] tmp = new Block[size+1];
	for (int i=0; i<size; i++)
	    tmp[i] = things[i];
	tmp[size] = p;
	things = tmp;
	repaint( p.x, p.y, p.w, p.h );
    }

    /**
       The painting callback. Translates into similar painting
       callbacks to all things paintable.
    **/
    public void paint(Graphics g)
    {
	if (things == null)
	    return;
	for (int i=0; i<things.length; i++)
	    things[i].paint(g);
	resized = false;
    }

    /**
       A filled rectangle .
    **/
    abstract class Block {

	int x;
	int y;
	int w;
	int h;
	Color color;
	boolean painted = false;

	Block(Color c)
	{
	    color = c;
	}

	protected void paint(Graphics g)
	{
	    synchronized (this) {
		g.setColor(color);
		g.fillRect(x,y,w,h);
		painted = true;
		notifyAll();
	    }
	}

	void waitPainted() // Called within synchronized block
	{
	    painted = false;
	    while (!painted) {
		try {
		    wait();
		}
		catch (InterruptedException e) {}
	    }
	}

    }

    class Foundation extends Block {

	Foundation(Color c)
	{
	    super(c);
	    setup();
	}

	protected void setup()
	{
	    x = GAP;
	    y = VERTICAL_BASE + PIN_HEIGHT;
	    w = WIDTH - 2 * GAP;
	    h = MODULE_HEIGHT;
	}

    }

    class Pin extends Block {
	int i = -1;

	Pin(int i,Color c)
	{
	    super(c);
	    this.i = i;
	    setup();
	}

	protected void setup()
	{
	    x = center[i] - MODULE_WIDTH;
	    y = VERTICAL_BASE;
	    w = 2 * MODULE_WIDTH;
	    h = PIN_HEIGHT;
	}

    }

    class Disc extends Block {

	int target;
	int target_x;
	int target_y;

	int pin;
	int number;
	int lvl;
	Disc next;

	synchronized void pickUp(int t)
	{
	    target = t;
	    target_x = xpos(target,number);
	    pins[pin] = next;
	    pin = -1;
	}

	synchronized void putDown(int p)
	{
	    lvl = level(p);
	    next = pins[p];
	    pins[p] = this;
	    pin = p;
	}

	protected void setup()
	{
	    target_x = xpos(target,number);
	    if (pin != -1) {
		x = xpos(pin,number);
		y = VERTICAL_BASE + lvl * MODULE_HEIGHT;
	    } else {
		// Use dx instead 
		x *= WIDTH / (WIDTH - dx);
		y *= HEIGHT / (HEIGHT - dy);
	    }
	    w = offset(number) * 2;
	    h = MODULE_HEIGHT;
	}

	Disc(int num,int p)
	{
	    super(Color.black);
	    number = num;
	    pin = p;
	    lvl = level(p);
	    next = pins[p];
	    pins[p] = this;
	    setup();
	}

	void move(int dx,int dy)
	{
	    synchronized (this) {
		repaint(x,y,w,h);
		x += dx;
		y += dy;
                repaint(x,y,w,h);
		waitPainted();
	    }
	    try {
		Thread.sleep(10); // slow down
	    }
	    catch (InterruptedException e) { }
	}
    }

    Disc [] pins = new Disc[3];

    /**
       Returns the next free module level on a pin.
    **/
    int level(int p)
    {
	return (pins[p]==null)? (LARGEST + 1) : pins[p].lvl - 1;
    }

    /**
       Returns a width pixel offset for a given disc.
    **/
    int offset(int num)
    {
	return MODULE_WIDTH * (LARGEST - num + 2);
    }

    /**
       Returns the home point x coordinate for a given disc on a given
       pin.
    **/
    int xpos(int p,int num)
    {
	return center[p] - offset(num);
    }

    /**
       Returns the home point y coordinate for a given disc on a given
       pin.
    **/
    int ypos(int p,int num)
    {
	return VERTICAL_BASE + level(p) * MODULE_HEIGHT;
    }

    public void addDisc(int disc,int p)
	throws Exception
    {
	if (disc < 1)
	    return;
	if (p < 0 || p > 2)
	    throw new ArrayIndexOutOfBoundsException("No such pin: "+p);

	addThing(new Disc(disc,p));
    }

    public Action moveDisc(int disc,int to)
	throws Exception
    {
	for (int from = 0; from < 3; from++) {
	    if (to == from)
		continue;
	    if (pins[from] == null)
		continue;
	    Disc d = (Disc)pins[from];
	    if (d.number == disc) {
		return new MovingDisc(tp,from,to);
	    }
	}
	throw new IllegalAccessException("Not top disc: "+disc);
    }

    class MovingDisc extends Action {

	int from;
	int target;

	MovingDisc(ThreadPool tp,int f,int to)
	{
	    super(tp); // Use local ThreadPool
	    from = f;
	    target = to;
	}

	protected void action()
	{
	    //System.out.println("moving from "+from+" to "+target);

	    // Remove from pin
	    Disc disc = pins[from];
	    synchronized ( Drawable.this ) {
		disc.pickUp(target);

		// Lift disc up above pins
		while (disc.y > VERTICAL_BASE - 2 * MODULE_HEIGHT)
		    disc.move(0,-MODULE_HEIGHT);

		// Shift to target pin
		int dir = (from < target)? MODULE_WIDTH : -MODULE_WIDTH;
		while (disc.x != disc.target_x)
		    disc.move(dir,0);

		// Sink down
		disc.target_y = ypos(target,disc.number);

		while (disc.y < disc.target_y)
		    disc.move(0,MODULE_HEIGHT);

		disc.putDown(target);
		//System.out.println("done moving from "+from+" to "+target);
	    }
	}
    }

    Drawable(Towers w)
    {
	window = w;
	setSize(WIDTH,HEIGHT);
	setup();

	// set background white.
	setBackground(Color.white);

	// Foundation
	addThing(new Foundation(Color.orange));

	// Pins
	for (int i=0; i<3; i++)
	    addThing(new Pin(i,Color.orange));
    }
}
