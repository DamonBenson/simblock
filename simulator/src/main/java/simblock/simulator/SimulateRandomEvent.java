package simblock.simulator;

import simblock.node.Node;

import static simblock.settings.SimulationConfiguration.NOEXTRA;
import static simblock.simulator.Main.random;


public class SimulateRandomEvent {
    /**
     * bandwith Decline because of local trouble.
     *
     * @param bandwith    the Bandwith of the node
     * @param investment     the investment of connections ,-1 means default investment
     */
    //TODO
    public static void bandwithDecline(long bandwith, long investment) {}

    /**
     * processingTime occupy more Extra time because of local trouble.
     *
     * @param processTime    the processTime of the node
     * @param investment     the investment of connections ,-1 means default investment
     */
    //TODO implement investment
    public static long processingTimeExtra(long processTime, long investment) {
        if (NOEXTRA){
            return processTime;
        }
        long simulatedProcessTime = processTime;
        long NearlyPositive = -1;
        while (NearlyPositive < 0) {//find a positive time const
            NearlyPositive = Math.round((random.nextGaussian() + 4));
        }
        // related event check
        if (crashEvent()) {//CrashRate 0.26%
            simulatedProcessTime += NearlyPositive * 800;//0-6400 ave:3200
            return simulatedProcessTime;
        }
        if (networkBusyEvent()) {//NetWorkBusyRate 4.55%
            simulatedProcessTime += NearlyPositive * 80;//0-640 ave:320
            return simulatedProcessTime;
        }
        if (localBusyEvent()) {//busyRate 50%
            simulatedProcessTime += NearlyPositive * 2;//0-18 ave:9
            return simulatedProcessTime;
        }
        return simulatedProcessTime;//No Problem
    }
    /**
     * Is localBusyEvent Happened
     * busyRate 50%
     */
    public static boolean localBusyEvent(){
        if(random.nextGaussian()>0) {
            return true;
        }
        return false;
    }

    /**
     * Is networkBusyEvent Happened
     * NetWorkBusyRate 4.55%
     */
    public static boolean networkBusyEvent(){
        if(random.nextGaussian()>0) {
            return true;
        }
        return false;
    }

    /**
     * Is crashEvent Happened
     * CrashRate 0.26%
     */
    public static boolean crashEvent(){
        if(random.nextGaussian()>0) {
            return true;
        }
        return false;
    }

}
