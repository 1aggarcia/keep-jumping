// source: pingPong.proto
/**
 * @fileoverview
 * @enhanceable
 * @suppress {missingRequire} reports error on implicit type usages.
 * @suppress {messageConventions} JS Compiler reports an error if a variable or
 *     field starts with 'MSG_' and isn't a translatable message.
 * @public
 */
// GENERATED CODE -- DO NOT EDIT!
/* eslint-disable */
// @ts-nocheck

goog.provide('proto.PingPong');
goog.provide('proto.PingPong.PayloadCase');

goog.require('jspb.BinaryReader');
goog.require('jspb.BinaryWriter');
goog.require('jspb.Message');
goog.require('proto.Ping');
goog.require('proto.Pong');

/**
 * Generated by JsPbCodeGenerator.
 * @param {Array=} opt_data Optional initial data array, typically from a
 * server response, or constructed directly in Javascript. The array is used
 * in place and becomes part of the constructed object. It is not cloned.
 * If no data is provided, the constructed object will be empty, but still
 * valid.
 * @extends {jspb.Message}
 * @constructor
 */
proto.PingPong = function(opt_data) {
  jspb.Message.initialize(this, opt_data, 0, -1, null, proto.PingPong.oneofGroups_);
};
goog.inherits(proto.PingPong, jspb.Message);
if (goog.DEBUG && !COMPILED) {
  /**
   * @public
   * @override
   */
  proto.PingPong.displayName = 'proto.PingPong';
}

/**
 * Oneof group definitions for this message. Each group defines the field
 * numbers belonging to that group. When of these fields' value is set, all
 * other fields in the group are cleared. During deserialization, if multiple
 * fields are encountered for a group, only the last value seen will be kept.
 * @private {!Array<!Array<number>>}
 * @const
 */
proto.PingPong.oneofGroups_ = [[1,2]];

/**
 * @enum {number}
 */
proto.PingPong.PayloadCase = {
  PAYLOAD_NOT_SET: 0,
  PING: 1,
  PONG: 2
};

/**
 * @return {proto.PingPong.PayloadCase}
 */
proto.PingPong.prototype.getPayloadCase = function() {
  return /** @type {proto.PingPong.PayloadCase} */(jspb.Message.computeOneofCase(this, proto.PingPong.oneofGroups_[0]));
};



if (jspb.Message.GENERATE_TO_OBJECT) {
/**
 * Creates an object representation of this proto.
 * Field names that are reserved in JavaScript and will be renamed to pb_name.
 * Optional fields that are not set will be set to undefined.
 * To access a reserved field use, foo.pb_<name>, eg, foo.pb_default.
 * For the list of reserved names please see:
 *     net/proto2/compiler/js/internal/generator.cc#kKeyword.
 * @param {boolean=} opt_includeInstance Deprecated. whether to include the
 *     JSPB instance for transitional soy proto support:
 *     http://goto/soy-param-migration
 * @return {!Object}
 */
proto.PingPong.prototype.toObject = function(opt_includeInstance) {
  return proto.PingPong.toObject(opt_includeInstance, this);
};


/**
 * Static version of the {@see toObject} method.
 * @param {boolean|undefined} includeInstance Deprecated. Whether to include
 *     the JSPB instance for transitional soy proto support:
 *     http://goto/soy-param-migration
 * @param {!proto.PingPong} msg The msg instance to transform.
 * @return {!Object}
 * @suppress {unusedLocalVariables} f is only used for nested messages
 */
proto.PingPong.toObject = function(includeInstance, msg) {
  var f, obj = {
    ping: (f = msg.getPing()) && proto.Ping.toObject(includeInstance, f),
    pong: (f = msg.getPong()) && proto.Pong.toObject(includeInstance, f)
  };

  if (includeInstance) {
    obj.$jspbMessageInstance = msg;
  }
  return obj;
};
}


/**
 * Deserializes binary data (in protobuf wire format).
 * @param {jspb.ByteSource} bytes The bytes to deserialize.
 * @return {!proto.PingPong}
 */
proto.PingPong.deserializeBinary = function(bytes) {
  var reader = new jspb.BinaryReader(bytes);
  var msg = new proto.PingPong;
  return proto.PingPong.deserializeBinaryFromReader(msg, reader);
};


/**
 * Deserializes binary data (in protobuf wire format) from the
 * given reader into the given message object.
 * @param {!proto.PingPong} msg The message object to deserialize into.
 * @param {!jspb.BinaryReader} reader The BinaryReader to use.
 * @return {!proto.PingPong}
 */
proto.PingPong.deserializeBinaryFromReader = function(msg, reader) {
  while (reader.nextField()) {
    if (reader.isEndGroup()) {
      break;
    }
    var field = reader.getFieldNumber();
    switch (field) {
    case 1:
      var value = new proto.Ping;
      reader.readMessage(value,proto.Ping.deserializeBinaryFromReader);
      msg.setPing(value);
      break;
    case 2:
      var value = new proto.Pong;
      reader.readMessage(value,proto.Pong.deserializeBinaryFromReader);
      msg.setPong(value);
      break;
    default:
      reader.skipField();
      break;
    }
  }
  return msg;
};


/**
 * Serializes the message to binary data (in protobuf wire format).
 * @return {!Uint8Array}
 */
proto.PingPong.prototype.serializeBinary = function() {
  var writer = new jspb.BinaryWriter();
  proto.PingPong.serializeBinaryToWriter(this, writer);
  return writer.getResultBuffer();
};


/**
 * Serializes the given message to binary data (in protobuf wire
 * format), writing to the given BinaryWriter.
 * @param {!proto.PingPong} message
 * @param {!jspb.BinaryWriter} writer
 * @suppress {unusedLocalVariables} f is only used for nested messages
 */
proto.PingPong.serializeBinaryToWriter = function(message, writer) {
  var f = undefined;
  f = message.getPing();
  if (f != null) {
    writer.writeMessage(
      1,
      f,
      proto.Ping.serializeBinaryToWriter
    );
  }
  f = message.getPong();
  if (f != null) {
    writer.writeMessage(
      2,
      f,
      proto.Pong.serializeBinaryToWriter
    );
  }
};


/**
 * optional Ping ping = 1;
 * @return {?proto.Ping}
 */
proto.PingPong.prototype.getPing = function() {
  return /** @type{?proto.Ping} */ (
    jspb.Message.getWrapperField(this, proto.Ping, 1));
};


/**
 * @param {?proto.Ping|undefined} value
 * @return {!proto.PingPong} returns this
*/
proto.PingPong.prototype.setPing = function(value) {
  return jspb.Message.setOneofWrapperField(this, 1, proto.PingPong.oneofGroups_[0], value);
};


/**
 * Clears the message field making it undefined.
 * @return {!proto.PingPong} returns this
 */
proto.PingPong.prototype.clearPing = function() {
  return this.setPing(undefined);
};


/**
 * Returns whether this field is set.
 * @return {boolean}
 */
proto.PingPong.prototype.hasPing = function() {
  return jspb.Message.getField(this, 1) != null;
};


/**
 * optional Pong pong = 2;
 * @return {?proto.Pong}
 */
proto.PingPong.prototype.getPong = function() {
  return /** @type{?proto.Pong} */ (
    jspb.Message.getWrapperField(this, proto.Pong, 2));
};


/**
 * @param {?proto.Pong|undefined} value
 * @return {!proto.PingPong} returns this
*/
proto.PingPong.prototype.setPong = function(value) {
  return jspb.Message.setOneofWrapperField(this, 2, proto.PingPong.oneofGroups_[0], value);
};


/**
 * Clears the message field making it undefined.
 * @return {!proto.PingPong} returns this
 */
proto.PingPong.prototype.clearPong = function() {
  return this.setPong(undefined);
};


/**
 * Returns whether this field is set.
 * @return {boolean}
 */
proto.PingPong.prototype.hasPong = function() {
  return jspb.Message.getField(this, 2) != null;
};


