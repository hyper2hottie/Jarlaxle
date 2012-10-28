package realms.jarlaxle.movingcircle;


import java.util.LinkedList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MovingCircleView extends SurfaceView implements SurfaceHolder.Callback {
	public class MovingCircleThread extends Thread
	{
		/*
		 * CONSTANTS
		 */
		/** Width of the circle in pixels */
		public static final int CIRCLE_WIDTH = 50;
		
		/** Color of the circle */
		public static final int CIRCLE_COLOR = 0xffff0000;
		
		/*
		 * State fields
		 */
		/** Height of the canvas */
		private int canvasHeight = 1;
		
		/** Width of the canvas */
		private int canvasWidth = 1;
		
		/** Center of canvas */
		private int cX;
		private int cY;
		
		/** The moving circle to draw */
		private ShapeDrawable circle;
		
		/** The boundary circle to draw, indicates edge of range. */
		private ShapeDrawable boundaryCircle;
		
		/** Location of circle */
		private int X;
		private int Y;
		
		/** Radius of movement. */
		private int radius;
		
		/** Boolean to decide to change the circle offset or not */
		private boolean updateOffset;
		
		/** Circle direction and velocity */
		private int vX;
		private int vY;
				
		/** Current animation state */
		private int state;
		
		/** Is thread runing */
		private boolean isRunning = false;
		
		/** Hanlder to the surface holder */
		private SurfaceHolder surfaceHolder;
				
		/** 
		 * Constructs the thread
		 * 
		 * @param surfaceHolder - The surface holder we interact with for drawing on.
		 */
		public MovingCircleThread(SurfaceHolder surfaceHolder)
		{
			//Initially assume no touch events
			updateOffset = true;
			
			//Set the handle to the surface holder
			this.surfaceHolder = surfaceHolder;
			
			//Create the circle
			circle = new ShapeDrawable(new OvalShape());			
			circle.getPaint().setColor(CIRCLE_COLOR);
			
			//Create a circle for the boundary
			boundaryCircle = new ShapeDrawable(new OvalShape());
			boundaryCircle.getPaint().setColor(0xFF0000FF);
			
			vX = 1;
			vY = 1;
		}
		
		@Override
		public void run()
		{
			while(isRunning)
			{						
				//Change offset if necessary
				if(updateOffset)
					decreaseOffset();
				
				//Update and draw the circle
				Canvas c = null;
				try
				{
					c = surfaceHolder.lockCanvas(null);
					synchronized (surfaceHolder) {
						updateCircleLocation();
						doDraw(c);
					}
				}
				catch(NullPointerException e)
				{}
				finally
				{
					if(c != null)
						surfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
		
		/** Used to change surface dimensions. */
		public void setSurfaceSize(int width, int height)
		{
			//synched to make sure changes atomically
			synchronized (surfaceHolder) {
				canvasHeight = height;
				canvasWidth = width;
				
				cX = X = canvasWidth/2;
				cY = Y = canvasHeight/2;
				
				//Set location of circle to be middle
				int left = canvasWidth/2 - CIRCLE_WIDTH/2;
				int top = canvasHeight/2 + CIRCLE_WIDTH/2;
				circle.setBounds(left, top, left + CIRCLE_WIDTH, top + CIRCLE_WIDTH);
				
				//Set the radius of motion
				radius = 255;
				radius = (radius > (Math.min(canvasHeight, canvasWidth)/2)) ? Math.min(canvasHeight, canvasWidth) / 2: radius;
				if(radius < 0)
					radius = 0;

				//Set the boundary circle based on radius 
				boundaryCircle.setBounds(cX - radius, cY - radius, cX + radius, cY + radius);
			}
		}
		
		/**
		 * Used to signal the thread to run or not.
		 * True lets it run, false will shut down the thread.
		 * 
		 * @param run - true to run, false to shut down
		 */
		public void setRunning(boolean run)
		{
			isRunning = run;
		}
		
		/**
		 * Draws the background and circle.
		 */
		private void doDraw(Canvas canvas)
		{
			//Black out background
			canvas.drawColor(0xff000000);
			
			//Draw boundary circle
			boundaryCircle.draw(canvas);
			
			//Draw the circle
			circle.draw(canvas);
		}
		
		/**
		 * Updates the location of the circle.
		 */
		/*private void updateCircle()
		{
			circle.getPaint().setColor(CIRCLE_COLOR);
			
			//Move the circle
			if(X > (cX + 100))
				vX = -1;
			else if(X < (cX - 100))
				vX = 1;
			
			if(Y > (cY + 100))
				vY = -1;
			else if(Y < (cY - 100))
				vY = 1;
			
			X += vX*2;
			Y += vY*2;
			//Set location of circle to be middle
			int left = X - CIRCLE_WIDTH/2;
			int top = Y - CIRCLE_WIDTH/2;
			circle.setBounds(left, top, left + CIRCLE_WIDTH, top + CIRCLE_WIDTH);
		}*/
		
		/**
		 * Update circle location.  Sets the circle objects location based
		 * on the location stored in the thread.
		 */
		private void updateCircleLocation()
		{
			//Set location of circle to be middle
			int left = X - CIRCLE_WIDTH/2;
			int top = Y - CIRCLE_WIDTH/2;
			if(circle.getBounds().left != left || circle.getBounds().top != top)
			{
				Pair<Integer, Integer> offset =  getCircleOffset();
				callListeners(offset.first, offset.second);
			}
			circle.setBounds(left, top, left + CIRCLE_WIDTH, top + CIRCLE_WIDTH);
		}
		
		/**
		 * Change the current location of the circle.  These changes only
		 * affect the draw location if the thread is running and updateCircleLocation()
		 * is being called.
		 */
		public void setCircleLocation(int x, int y)
		{
			double finalX = x - cX;
			double finalY = y - cY;
			if((finalX*finalX + finalY*finalY) > radius * radius)
			{
				double tempX = finalX;
				double tempY = finalY;
				
				//Slope of the line
				double m = tempY/tempX;
				
				//Compute the absolute value of x
				double absX = Math.sqrt((radius * radius)/(m*m + 1));
				finalX = (tempX < 0) ? absX * -1 : absX;
				finalY = m * finalX;				
				
			}
			synchronized (surfaceHolder) {
				X = ((int) finalX + cX);
				Y = ((int) finalY + cY);
			}
		}
		
		/**
		 * Decrease offset.
		 * Moves the circle in towards the center.  Used 
		 * to auto-center the circle.
		 */
		private void decreaseOffset()
		{
			//Get the current x and y values
			int curX = X - cX;
			int curY = cY - Y;
			
			//Current radius
			double curR = Math.sqrt(curX*curX + curY * curY);
			
			//The rate at which the cirle auto centers
			int rate = 0;
						
			//When more than two thirds out, decrease radius by 3 each run through
			if(curR > radius * 9/10)
				rate = 7;
			else if(curR > radius * 8/10)
				rate = 6;
			else if(curR > radius * 7/10)
				rate = 5;
			else if(curR > radius * 5/10)
				rate = 4;
			else if(curR > radius * 3/10)
				rate = 3;
			else if(curR > radius * 2/10) //rate of 2
				rate = 2;
			else if(curR > 0.000000000000001) //rate of 0
				rate = 1;
			else
				rate = 0;
			
			//Slope of line
			double m = ((double)curY)/((double)curX);
			
			//New radius
			double newR = curR - rate;
			
			//X value for the circle
			double finalX = Math.sqrt((newR * newR)/(m * m + 1));
			finalX = (curX < 1) ? finalX * -1: finalX;
			
			//Y value for circle
			double finalY = m * finalX;
			
			//Set values for the circle
			X = (int) (Math.round(finalX) + cX);
			Y = (int) (cY - Math.round(finalY));
		}
		
		/**
		 * Get circle offset.  Used to get the offset from center of the screen
		 * Note: Towards the right of the screen is a positive x offset and towards 
		 * the top is a positive y offset.
		 * @return offset of drawn circle in format [x,y] from center of screen
		 */
		public Pair<Integer, Integer> getCircleOffset()
		{
			int tempX = X;
			int tempY = Y;
			int tempcX = cX;
			int tempcY = cY;
			
			return new Pair<Integer, Integer>(tempX-tempcX, tempcY - tempY);
		}
	}
	
	/** The thread that animates the circle */
	private MovingCircleThread thread;	
	
	Context context;
	

	/** Listeners */
	private LinkedList<MovingCircleListener> listeners;
	
	/** 
	 * Return the circle thread 
	 * 
	 * @return the animation thread
	 */
	public MovingCircleThread getThread()
	{
		return thread;
	}
	
	public MovingCircleView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		//register that we want to head changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
				
		//create the listeners list
		listeners = new LinkedList<MovingCircleListener>();
		
		setFocusable(true);
	}
	
	/**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
    }

	public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height) {
		thread.setSurfaceSize(width, height);
		
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
		thread = new MovingCircleThread(holder);
		thread.setRunning(true);
        if(!thread.isAlive())
        	thread.start();
        else
        {
        	synchronized (thread) {
        		thread.notifyAll();
			}
        }
		
	}

	/**
	 * The surface is destroyed once this returns, stop the thread from trying to touch it
	 * holder - The holder that holds the surface
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		//stop the thread so it doesn't try to access the destroyed surface
		boolean retry = true;
		thread.setRunning(false);
		while(retry)
		{
			try
			{
				thread.join();
				retry = false;
			}
			catch(InterruptedException e)
			{}
		}
		
	}	
	
	/**
	 * This function is used for adding a listener.  All listeners will
	 * be called whenever the circles location changes
	 * @param listener - the object that wants calls from this circle
	 */
	public void registerListener(MovingCircleListener listener)
	{
		if(!listeners.contains(listener))
			listeners.add(listener);
	}
	
	/**
	 * This function removes a listener.
	 * @param listener - the object that no longer wants to recieve updates
	 */
	public void unregisterListener(MovingCircleListener listener)
	{
		if(listeners.contains(listener))
			listeners.remove(listener);
	}
	
	/**
	 * Call all of the listeners with the new circle location.
	 */
	private void callListeners(int X, int Y)
	{
		for(MovingCircleListener listener : listeners)
		{
			listener.onCircleMoved(X, Y);
		}
	}
	
	/**
	 * Whenever someone makes a touch to this view the onTouchEvent
	 * is called.  It updates the location of the circle.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if(event.getAction() == MotionEvent.ACTION_DOWN)
			thread.updateOffset = false;
		else if(event.getAction() == MotionEvent.ACTION_UP)
			thread.updateOffset = true;
		int x = (int) event.getX();
		int y = (int) event.getY();
		thread.setCircleLocation(x, y);
		return true;
	}
}
