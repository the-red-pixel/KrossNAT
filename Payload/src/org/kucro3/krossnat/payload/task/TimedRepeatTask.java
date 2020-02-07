package org.kucro3.krossnat.payload.task;

import com.theredpixelteam.redtea.util.Predication;

public abstract class TimedRepeatTask extends AbstractTask {
    public TimedRepeatTask(long delayMillis)
    {
        this.timeStamp = System.currentTimeMillis();
        this.delayMillis = Predication.requirePositive(delayMillis);
    }

    @Override
    public void onRequeue()
    {
        this.timeStamp = System.currentTimeMillis();
    }

    @Override
    public boolean shouldExecute()
    {
        return (System.currentTimeMillis() - timeStamp) >= delayMillis;
    }

    @Override
    public boolean shouldRequeue()
    {
        return true;
    }

    public long getDelayMillis()
    {
        return delayMillis;
    }

    public void setDelayMillis(long delayMillis)
    {
        this.delayMillis = Predication.requirePositive(delayMillis);
    }

    @Override
    public abstract void run() throws Exception;

    public static TimedRepeatTask of(Runnable runnable, long delayMillis)
    {
        return new TimedRepeatTask(delayMillis) {
            @Override
            public void run() throws Exception
            {
                runnable.run();
            }
        };
    }

    private volatile long delayMillis;

    private volatile long timeStamp;
}
