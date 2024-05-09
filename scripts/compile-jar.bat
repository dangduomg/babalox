@echo off
cd ..
javac -verbose -d build -cp src src/com/craftinginterpreters/lox/*
jar -c -v -f bin/jlox.jar -e com.craftinginterpreters.lox.Lox -C build .
