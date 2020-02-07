package org.kucro3.krossnat.payload.task;

import java.util.concurrent.ConcurrentLinkedDeque;

public class TaskQueue {
    public void queue(Task task)
    {
        task.onRequeue();
        taskQueue.addLast(task);
    }

    public Task pop()
    {
        return taskQueue.pop();
    }

    public int count()
    {
        return taskQueue.size();
    }

    private final ConcurrentLinkedDeque<Task> taskQueue = new ConcurrentLinkedDeque<>();
}
