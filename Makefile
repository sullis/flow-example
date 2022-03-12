clean:
	mvn clean

run:
	mvn package
	java -jar server/target/app.jar server conf/config.yml
