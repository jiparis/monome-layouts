//led position
var matrix;

var note;
var res;

var step;

function init(){
  out.println("Loading sequencer");
  matrix = new Array(jsobject.getWidth());   
  for (var i = 0; i < jsobject.getWidth(); i++){
	 matrix[i] = new Array(jsobject.getHeight());
  }
  note = parseInt(jsargs.get("note"));
  res = parseInt(jsargs.get("res"));
  
  step = 0;
}

function notify(pressed, x, y){	
	if (pressed) matrix[x][y] = !matrix[x][y];
	jsobject.refresh();
}

function writeOn(frame){
	for (var i = 0; i < jsobject.getWidth(); i++){
	  for (var j = 0; j < jsobject.getHeight(); j++){
	  	frame.set(i, j, matrix[i][j]? LED_ON: LED_OFF);	  	
	  }
	  frame.set(i, step, LED_ON);
	}	
}

function start(){
	step = 0;
	//send first midi seq
	for (var i = 0; i < jsobject.getWidth(); i++){
		if (matrix[i][0])
			midi.sendNoteOn(jsobject.getChannel(), parseInt(i) + note, 127);
	}
}

function clkReceived(){	
	step++;
	if (step == jsobject.getHeight())
		step = 0;
	jsobject.refresh();
	//send midi
	for (var i = 0; i < jsobject.getWidth(); i++){
		if (matrix[i][step])
			midi.sendNoteOn(jsobject.getChannel(), parseInt(i) + note, 127);
	}
}