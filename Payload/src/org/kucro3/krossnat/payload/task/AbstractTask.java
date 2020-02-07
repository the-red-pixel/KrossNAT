package org.kucro3.krossnat.payload.task;

public abstract class AbstractTask implements Task {
    protected AbstractTask()
    {
    }

    @Override
    public TaskExceptionHandler getExceptionHandler()
    {
        return exceptionHandler;
    }

    @Override
    public void setExceptionHandler(TaskExceptionHandler exceptionHandler)
    {
        this.exceptionHandler = exceptionHandler;
    }

    protected TaskExceptionHandler exceptionHandler;
}
