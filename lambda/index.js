var net = require('net');
var error = function (data) {
  console.log('Error ' + data);
};

const SYSLOG_REGEX = /^(\w+\s\d+\s[\d+:]+)\s(.+?)\s(.+?)(?:\[(\d+)\])?:\s(.*)$/i;

var jsonMessageToObject = function (jsonMessage) {
  var parsedMessage, timestamp;

  try {
    parsedMessage = JSON.parse(jsonMessage);
    timestamp = parsedMessage.timestamp;

    parsedMessage.timestamp = new Date(timestamp).toISOString();
  } catch (e) {
  }

  return parsedMessage;
};

var logEventToElasticSearchEvent = function (rawMessage) {
  var result = {};
  var source = {};
  var segments = rawMessage.match(SYSLOG_REGEX);

  if (segments) {
    source.message = segments[5];
    source.hostname = segments[2];
    source.process = segments[3];
    if (segments[4]) {
      source.pid = segments[4];
    }

    // Processes with a process name of 'docker/<image>' extract image name
    var dockerSegments = source.process.match(/docker\/(.*)/);
    if (dockerSegments !== null) {
      source.container_image = dockerSegments[1];
    }

    var parsedMessage = jsonMessageToObject (segments[5]);
    if (parsedMessage) {
      for (var attrname in parsedMessage) {
        source[attrname] = parsedMessage[attrname];
      }
    }

  } else {
    source.message = rawMessage;
  }

  source.raw = rawMessage;

  // Prepend @ to all keys
  for (var attrname in source) {
    result['@' + attrname] = source[attrname];
  }
  return result;
}

var lambda = function (message) {
  return logEventToElasticSearchEvent(message);
};

var server = net.createServer(function(socket){
  socket.on('data', function (data) {
    var parsedData, message, result;
    console.log('Recieved ' + data);
    try{
      parsedData = JSON.parse(data.toString());
    }catch(e){
      console.log('Failed to parse ' + data);
      return false;
    }

    message = parsedData.message;
    result = lambda(message);

    client = net.connect({host: "default.docker", port: 5065}, function () {
      console.log('Connected to Logstash');
      var jsonResult = JSON.stringify(result);
      client.write(jsonResult);
      client.write("\n");
      client.end();
    });
    client.on('error', error);
  });
  socket.on('error', error);
});

server.listen({host: "0.0.0.0", port: 5055}, function () {
  console.log('Listening on 5055');
});
