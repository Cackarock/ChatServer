all:
	javac *.java

clean:
	rm -rf *.class

run_server:
	java ChatServer ${args}

run_client:
	java ChatClient
