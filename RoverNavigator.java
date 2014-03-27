import lejos.nxt.*;
import lejos.geom.Point;
import lejos.robotics.navigation.*;
import lejos.robotics.localization.OdometryPoseProvider;
import lejos.util.*;

/**
* Abertay University - SET
* Mars Rover Project
* RoverNavigator class
* 2014-03-25 Simon Stoll | Cesar Parent
*
* Provides abstraction layer for moving a rover along a simple coordinates system
* 
*/
public class RoverNavigator {
    
    private DifferentialPilot driveUnit;
    private InstrumentsKit instrumentsUnit;
    
    private OdometryPoseProvider poseProvider;
    
    private float targetX;
    private float targetY;
    
    private Rover rover;
    
    public RoverNavigator(DifferentialPilot pilot, InstrumentsKit mast) {
        
        this.driveUnit = pilot;
        this.instrumentsUnit = mast;
        
        this.poseProvider = new OdometryPoseProvider(this.driveUnit);
        
        this.targetX = 0.0f;
        this.targetY = 0.0f;
        
    }
    
    /**
    * Moves the rover to a point, unless stopped on its way
    *
    * @param float x the X coordinate of the target point
    * @param float y the Y coordinate of the target point
    * @return boolean true if the rover reached the points, false if it was stopped
    */
    public boolean moveToPoint(float x, float y) {
        
        // get the relative bearing and distance to the target point
        Point targetPoint = new Point(x, y);
        double bearing = (double) this.poseProvider.getPose().relativeBearing(targetPoint);
        float distance = this.poseProvider.getPose().distanceTo(targetPoint);
        
        // rotate the rover and launch the drive sequence
        this.rotateByAngle(bearing);
        boolean success = this.travelDistance(distance);
        return success;
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
            Delay.msDelay(10);
            shouldStop = (this.instrumentsUnit.obstacleAhead() || Button.ENTER.isDown()) ? true : false;
            
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
    * @return void
    */
    public void setHeading(double targetHeading) {
        
        double currentHeading = (double) this.getHeading();
        double neededRotation = currentHeading - targetHeading;
        
        this.driveUnit.rotate(neededRotation);
    }
    
    /**
    * Rotate the rover by an angle relative to its current heading
    *
    * @param double targetAngle the absolute angle (0 is along x, +90 along y)
    * @return void
    */
    public void rotateByAngle(double offsetAngle) {
        this.driveUnit.rotate(offsetAngle);
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
    * Displays the current state (position and heading) of the rover
    * 
    * @return String the navigator's state
    */
    public String toString() {
        return "X: "+(int)this.getX()+", Y: "+(int)this.getY()+", Hdg: "+(int)this.getHeading();
    }
    
    /**
    * Sets the main rover controller reference for the class
    * 
    * @param Object roverObject the instance of the main Rover Controller
    * @return void
    */
    public void setRoverObject(Rover roverObject) {
        this.rover = roverObject;
    }
}