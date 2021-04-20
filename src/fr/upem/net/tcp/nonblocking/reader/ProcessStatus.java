package fr.upem.net.tcp.nonblocking.reader;

/**
 * Represents the different states a process can be in :
 * DONE, if the process has completed normally
 * REFILL, if the process cannot complete because content is missing
 * ERROR, if an error has occurred
 */
public enum ProcessStatus {
	DONE,REFILL,ERROR
}
