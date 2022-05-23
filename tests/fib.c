var x: int;

fun fib(num: int): int = ({
  if num == 0 | num == 1 then
    result = num;
  else
    result = fib(num - 1) + fib(num - 2);
  end;

  result;
} where var result: int;);

fun main(): int = {
  fib(9);
};