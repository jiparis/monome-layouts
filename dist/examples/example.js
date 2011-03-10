
var state = false;

function init(){
  out.println(jsargs.get("arg1"));
}

function notify(pressed, x, y){
	state = pressed;
	jsobject.refresh();
	
}

function writeOn(frame){
	for (var i = 0; i< jsobject.getWidth(); i++){
		for (var j = 0; j< jsobject.getHeight(); j++){
			frame.set(i, j, state? LED_ON:LED_OFF);
		}
	}
}

function start(){
  out.println("start");
}
function stop(){
out.println("stop");
}

function clkReceived(){
	//out.println("tick");
}

function noteOnReceived(message){
  out.println("noteon: " + message.getData1() + ", " + message.getData2());
}
function noteOffReceived(message){
 out.println("noteoff: " + message.getData1() + ", " + message.getData2());
}