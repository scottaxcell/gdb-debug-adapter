#include <iostream>
using namespace std;

void foo();

int main() 
{
  cout << "Hello, World!" << endl;

  foo();

  for (int i = 0; i < 3; i++) {
    cout << "loop: " << i << endl;
  }

  cout << "Goodbye, Cruel World!" << endl;
  return 0;
}

void foo() {
  int numPlanets = 8;
  cout << "function foo" << endl;
}
