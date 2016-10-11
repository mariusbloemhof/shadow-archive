package com.servabosafe.shadow.utility.command;


/**
 * Command is a simple interface, that is used with custom AsyncTasks to bind
 * the returned data to the UI thread
 * 
 * @author Josh
 * 
 */
public interface Command
{
  /**
   * Called via an AsyncTask subclass, upon completion. Runs on the UI thread
   * 
   * @param data The result of the HTTP request
   */
  public void execute(Object data);
}