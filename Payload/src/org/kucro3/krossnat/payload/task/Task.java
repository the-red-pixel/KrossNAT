package org.kucro3.krossnat.payload.task;

public interface Task {
    public boolean shouldExecute();

    public boolean shouldRequeue();

    public default void onRequeue()
    {
    }

    public void run() throws Exception;

    public TaskExceptionHandler getExceptionHandler();

    public void setExceptionHandler(TaskExceptionHandler exceptionHandler);
}
