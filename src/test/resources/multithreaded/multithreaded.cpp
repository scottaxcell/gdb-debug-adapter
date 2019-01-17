#include <iostream>
#include <thread>
#include <vector>
#include <map>
using namespace std;

class MyClass {
public:
    int dataMember = 0;

    void printAndIncrDataMember() {
        cout << "MyClass dataMember is: " << ++dataMember << endl;
    }
};

void doSomethingElse(MyClass* myClass) {
    float f = 42.0;
    double double_d = 30.11;
    const char* c = "constant char string";
    vector<int> intVec;
    intVec.push_back(11);
    intVec.push_back(222);
    intVec.push_back(3333);
    map<char, int> myMap;
    myMap['a'] = 101;
    myMap['b'] = 2;
    myMap['c'] = 78;

    for (int qwerty = 0; qwerty < 10; qwerty++) {
        double some_double = 42.31;
        double another_double = 13.5243;
        myClass->printAndIncrDataMember();
    }
}

void doSomethingWithArgs(int intArg, float floatArg) {
    float localFloat = floatArg;
    int localInt = intArg;
    return;
}

void doSomething() {
    MyClass* myClass = new MyClass();
    doSomethingElse(myClass);
}

void foo() {
    float f = 42.0;
    double double_d = 30.11;
    const char* c = "constant char string";

    for (int i = 0; i < 10; i++) {
        int x = i * 3;
        int y = i + 1;
        int z = x + y;
        doSomethingWithArgs(x, f);
    }

    cout << "foo completed" << endl;
}

void fubar() {
    for (int i = 0; i < 1; i++) {
        int x = i * 30;
        int y = i + 42;
        int z = x + y;
        doSomething();
    }

    cout << "fubar completed" << endl;
}

int main() {
    cout << "Threading Example" << endl;

    thread first(foo);
    thread second(fubar);

    cout << "main, executing foo and fubar concurrently.." << endl;

    first.join();
    second.join();

    cout << "main, foo and fubar completed" << endl;

    return 0;
}
