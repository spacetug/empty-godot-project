extends Node



func _ready():
	var plugin = Engine.get_singleton("TestPlugin")
	if plugin:
		plugin.connect("message", _receive)
		
func _receive(bytes: PackedByteArray):
	print("received %d bytes" % bytes.size())
