## Performance Test
It's a little early to really talk about performance, but it's good to start early. 

The example project contains a [simple JMeter test case](example/jmeter-test.jmx) that runs the 4 basic HTTP requests the framework supports PUT, POST, GET and DELETE. The test runs several of these operations with 20 parallel threads. The test runs a total of 1,000,000 requests.

### Testing with an in-memory database
On an 8 core machine the test is running about 1:30 minutes for 1,000,000 read and write operations. 

The server is able to execute
- more than 750 requests per second on requests doing a lookup and write operation, 
- more than 1500 requests per second on a simple write and 
- more then 4500 requests per second on a read operation. 

Memory consumption jumped to approx. 330MB, but the GC seems to be able to clean up very nicely.

The response times are quite good
- median is 1-2ms and 
- 99% is 2-3ms.

Running the garbage collector after the test, the heap size dropped to about 40 MByte.

### Testing with a Postgresql database and a bit more complex model
On an 8 core machine the test is running about 31 minutes for 1,000,000 read and write operations. 

The server is able to execute
- more than 50 requests per second on requests doing a lookup and write operation, 
- more than 100 requests per second on a simple write and 
- more then 300 requests per second on a read operation. 

Memory consumption jumped to approx. 200MB, but the GC seems to be able to clean up very nicely.

The response times are quite good
- median is 2-3ms for PUT and POST, 60ms for GET, 15ms for DELETE 
- 99% is 7-8ms for PUT and POST, 110ms for GET and 55ms for DELETE.

Running the garbage collector after the test, the heap size dropped to about 40 MByte.
