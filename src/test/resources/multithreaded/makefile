CC = g++
CFLAGS = -g -pthread
RM = rm -f

default: all

all: multithreaded

multithreaded: multithreaded.cpp
	$(CC) $(CFLAGS) -o multithreaded multithreaded.cpp

clean:
	$(RM) multithreaded
