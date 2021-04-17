package simblock.simulator;

import simblock.node.Node;

import static simblock.settings.SimulationConfiguration.NOEXTRA;
import static simblock.simulator.Main.random;


public class SimulateRandomEvent {
  // boolean success = random.nextDouble() > CBRfailureRate ? true : false;
  /**
   * bandwith Decline because of local trouble.
   *
   * @param bandwith    the Bandwith of the node
   * @param investment     the investment of connections ,-1 means default investment
   */
  //TODO+
  public static void bandwithDecline(long bandwith, long investment) {}

  /**
   * processingTime occupy more Extra time because of local trouble.
   * compare to process status in node class,
   * @param processTime    the processTime of the node
   * @param investment     the investment of connections ,-1 means default investment
   */
  //TODO implement investment
  public static long processingTimeExtra(long processTime, long investment) {
    if (NOEXTRA){
      return processTime;
    }
    long simulatedProcessTime = processTime;//default is 2ms
    long PositiveRandom;
    PositiveRandom = Math.abs(Math.round((random.nextGaussian()+4)));//ave:4 div:0

    // related event check
    if (crashEvent()) {//CrashRate 0.26%
      simulatedProcessTime += PositiveRandom * 800;//2-6402 ave:3202  800
      return simulatedProcessTime;
    }
    if (networkBusyEvent()) {//NetWorkBusyRate 4.55%
      simulatedProcessTime += PositiveRandom * 80;//2-642 ave:322  80
    }
    if (localBusyEvent()) {//busyRate 50%
      simulatedProcessTime += PositiveRandom * 2;//2-18 ave:10  2
    }

    return simulatedProcessTime;//Problem make time more
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
    if(random.nextGaussian()>2) {
      return true;
    }
    return false;
  }

  /**
   * Is crashEvent Happened
   * CrashRate (100% - 99.9936% )/2
   */
  public static boolean crashEvent(){
    if(random.nextGaussian()>4) {
      return true;
    }
    return false;
  }

}
