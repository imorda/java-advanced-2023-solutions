javac -cp ..\java-solutions;..\lib\* ^
      -d out ^
      ..\java-solutions\info\kgeorgiy\ja\belousov\bank\* && ^
java -cp out;..\lib\* ^
      org.junit.runner.JUnitCore ^
      info.kgeorgiy.ja.belousov.bank.BankTests
