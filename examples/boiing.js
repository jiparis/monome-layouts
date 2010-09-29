//led position
var matrix;

// directions
var directions;

//pressed buttons position
var values;

var note;

function init(){
  out.println("Loading boing.js");
  matrix = new Array(jsobject.getWidth()); 
  matrix = matrix.map(function(){ return -1; });
  
  values = new Array(jsobject.getWidth()); 
  values = values.map(function(){ return -1; });
  
  directions = new Array(jsobject.getWidth()); 
  directions = directions.map(function(){ return 1; });
  
  note = parseInt(jsargs.get("note"));
}

function writeOn(frame){
	for (var i = 0; i< jsobject.getWidth(); i++){
		for (var j = 0; j< jsobject.getHeight(); j++){
			frame.set(i, j, (matrix[i]==j)? LED_ON:LED_OFF);
		}
	}
}

function notify(pressed, x, y){
  if (pressed){
    values[x] = y;
    if (y == jsobject.getHeight() - 1){
      matrix[x] = -1;
      midi.sendNoteOff(jsobject.getChannel(), parseInt(x) + note);
    }else{
      matrix[x] = y;
      directions[x] = 1;
    }
  }
  jsobject.refresh();
}

function clkReceived(){
  for (var i = 0; i < jsobject.getWidth(); i++){
    var button = values[i];
  	if (button != jsobject.getHeight() - 1){
  	  matrix[i] = matrix[i] + directions[i];  	 
  	  if (matrix[i] == jsobject.getHeight() - 1){
  	    directions[i] = -1;  	    
  	    midi.sendNoteOn(jsobject.getChannel(), parseInt(i) + note, 127);
  	  }
  	  if (matrix[i] == button){
  	    directions[i] = 1;
  	    midi.sendNoteOff(jsobject.getChannel(), parseInt(i) + note);
  	  }
  	}
  }
  jsobject.refresh();
}
