#include <iostream>
using namespace std;

void foo();
int fubar();

int main() {
  cout << "Hello, World!" << endl;

  foo();

  for (int i = 0; i < 3; i++) {
    cout << "loop: " << i << endl;
  }

  int numFubars = fubar();
  cout << "# of fubars = " << numFubars << endl;

  cout << "Goodbye, World!" << endl;

  return 0;
}

void foo() {
  int numPlanets = 8;
  const char* nameOfThirdPlanet = "Earth";
  cout << "function foo called" << endl;
}

int fubar() {
  cout << "function fubar called" << endl;
  return -42;
}
