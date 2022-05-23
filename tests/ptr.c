fun putChar(c : char) : void = none;

var ptr: ^int;
var address: int;

fun main(): int = {

  ptr = (new 16: ^int);
  ptr^ = 65; 
  (((ptr: int) + 8): ^int)^ = 66; 

  address = (ptr : int); 

  putChar(((address: ^int)^ : char)); 
  putChar((10 : char));

  putChar((((address + 8): ^int)^ : char)); 
  putChar((10 : char));

  0;
};