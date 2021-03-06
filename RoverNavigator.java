import lejos.nxt.*;
import lejos.geom.Point;
import lejos.robotics.navigation.*;
import lejos.robotics.localization.OdometryPoseProvider;
import lejos.util.*;
import java.util.*;

/**
* Abertay University - SET
* Mars Rover Project
* RoverNavigator class
* 2014-03-25 Simon Stoll | Amy Parent
*
* Provides abstraction layer for moving a rover along a simple coordinates system
* 
*/
public class RoverNavigator {
    
    private LinkedList<Point> path;
    private Point nextPoint;
    
    protected DifferentialPilot driveUnit;
    protected InstrumentsKit instrumentsUnit;
    
    protected OdometryPoseProvider poseProvider;
    
    protected Rover rover;
    
    /**
    * Constructor
    * Takes control of the Instruments Unit and the Drive Unit
    *
    * @param DifferentialPilot pilot the Differential drive unit used to control the rover
    * @param InstrumentKit mast the rover's sensors array
    */
    public RoverNavigator(DifferentialPilot pilot, InstrumentsKit mast, Rover roverObject) {
        
        this.driveUnit = pilot;
        this.instrumentsUnit = mast;
        this.rover = roverObject;
        
        this.poseProvider = new OdometryPoseProvider(this.driveUnit);
        
        path = new LinkedList<Point>();
        
    }
    
    /**
    * Moves the rover to a point, unless stopped on its way
    *
    * @param float x the X coordinate of the target point
    * @param float y the Y coordinate of the target point
    * @return boolean true if the rover reached the points, false if it was stopped
    */
    public boolean goTo(float x, float y) {
        
        // get the relative bearing and distance to the target point
        Point targetPoint = new Point(x, y);
        double bearing = (double) this.poseProvider.getPose().relativeBearing(targetPoint);
        float distance = this.poseProvider.getPose().distanceTo(targetPoint);
        
        // rotate the rover and launch the drive sequence
        if(this.rotateBy(bearing)) {
            return this.travelDistance(distance);
        }
        else {
            return false;
        }
    }
    
    /**
    * moves the rover along its front/back axis by the given distance
    *
    * @param float targetDistance the distance the rover has to move.
                                  positive fowards, negative backwards
    * @return boolean true if the rover reached the distance, false if it was stopped
    */
    public boolean travelDistance(double targetDistance) {
        
        // start moving
        this.driveUnit.travel(targetDistance, true);
        
        // check for obstacles every .1 second
        // stop checking when an obstacle is encountered
        // or when the driving unit has stopped moving
        boolean shouldStop = false;
        do {
            shouldStop = this.instrumentsUnit.obstacleAhead();
        } while(this.driveUnit.isMoving() && !shouldStop);
        
        // return true or false depending on the reason for stopping
        if(shouldStop) {
            this.driveUnit.stop();
            return false;
        }
        else {
            return true;
        }
    }
    
    /**
    * Rotate the rover to an absolute angle relative to the coordinate system
    *
    * @param double targetHeading the absolute angle (0 is along x, +90 along y)
    * @return boolean true if the targetHeading was reached, false otherwise
    */
    public boolean rotateTo(double targetHeading) {
        
        double currentHeading = (double) this.getHeading();
        double neededRotation = (targetHeading - currentHeading)%360;
        
        return this.rotateBy(neededRotation) ? true : false;
    }
    
    /**
    * Rotate the rover by an angle relative to its current heading
    *
    * @param double targetAngle the absolute angle (0 is along x, +90 along -y)
    * @return void
    */
    public boolean rotateBy(double offsetAngle) {
        // turn drift correction
        double realAngle = offsetAngle < 0.0 ? offsetAngle + 0 : offsetAngle - 0;
        this.driveUnit.rotate(realAngle, true);
        // check for obstacles as we turn
        boolean shouldStop = false;
        do {
            Delay.msDelay(10);
            shouldStop = this.instrumentsUnit.bumpersTriggered();
        } while(this.driveUnit.isMoving() && !shouldStop);
        
        // return true or false depending on the reason for stopping
        if(shouldStop) {
            this.driveUnit.stop();
            return false;
        }
        else {
            return true;
        }
    }
    
    /**
    * Returns the rover's current X coordinate
    *
    * @return float rover's coordinate along the x axis
    */
    public float getX() {
        return this.poseProvider.getPose().getX();
    }
    
    /**
    * Returns the rover's current Y coordinate
    *
    * @return float rover's coordinate along the y axis
    */
    public float getY() {
        return this.poseProvider.getPose().getY();
    }
    
    /**
    * Returns the rover's absolute heading
    *
    * @return float the rover's heading
    */
    public float getHeading() {
        return this.poseProvider.getPose().getHeading();
    }
    
    /**
    * Returns the pos object of the navigator
    * 
    * @return Pose the current pose, returned from the odometryPoseProvider
    */
    public Pose getPose() {
        return this.poseProvider.getPose();
    }
    
    /**
    * Displays the current state (position and heading) of the rover
    * 
    * @return String the navigator's state
    */
    public String toString() {
        return "X: "+(int)this.getX()+", Y: "+(int)this.getY()+", Hdg: "+(int)this.getHeading();
    }
    
    /*
    ##############################
    Advanced functions
    */
    
    /**
    * Adds a new waypoint at the end of the path
    *
    * @param float x new waypoint's x coordinate
    * @param float y new waypoint's y coordinate
    */
    public void addWaypoint(float x, float y) {
        
        Point newPoint = new Point(x, y);
        //insert the point at the very end of the list
        this.path.add(newPoint);
    }
    
    /**
    * Fushes the path of the rover
    *
    * @return void
    */
    public void clearPath() {
        this.path.clear();
    }
    
    /**
    * Commands the rover to follow the path in memory
    * The rover will go on until an obstacle is encountered
    * In that case, the rest of the path will stay in memory.
    *
    * @return boolean true if the last point was reached, false otherwise
    */
    public boolean followPath() {
        
        // copy the list of waypoints to an array, and iterate over it
        Point[] pathArray = this.path.toArray(new Point[this.path.size()]);
        for(int i = 0; i < pathArray.length; i++) {
            
            Point target = pathArray[i];
            if(!this.goTo((float)target.getX(), (float)target.getY())) {
                return false;
            }
            else {
                // the waypoint was reached, remove it from the list
                this.rover.arrivedAtWaypoint();
                this.path.remove(target);
            }
        }
        // if the path was empty, return true
        return true;
    }
}