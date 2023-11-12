package org.example;

public class ProgressInfo {
    private long currentBytes;
    private long totalBytes;
    private boolean paused;
    private boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public ProgressInfo() {
        this.currentBytes = 0;
        this.totalBytes = 0;
        this.paused = false;
        this.canceled = false;
    }

    public ProgressInfo(long currentBytes, long totalBytes) {
        this.currentBytes = currentBytes;
        this.totalBytes = totalBytes;
        this.paused = false;
        this.canceled = false;
    }


    public long getCurrentBytes() {
        return currentBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setCurrentBytes(long currentBytes) {
        this.currentBytes = currentBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }
}