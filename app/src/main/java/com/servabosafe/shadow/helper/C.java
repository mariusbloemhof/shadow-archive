package com.servabosafe.shadow.helper;

/**
 * The C(onstants) class
 * 
 * @author Josh
 */
public final class C
{
    public static final boolean SHOULD_SHOW_LOG_OUTPUT = true;
    public static final String TAG = "ServaboSafe";

    public static enum MARKET
  {
    GOOGLE,
    AMAZON,
    OTHER
  }

  // this is used to check for version updates
  public static final MARKET  WHICH_MARKET                        = MARKET.GOOGLE;

}
