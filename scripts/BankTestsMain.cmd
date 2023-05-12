javac -cp ..\java-solutions;..\lib\* ^
      -d out ^
      ..\java-solutions\info\kgeorgiy\ja\belousov\bank\* && ^
java -cp out;..\lib\* ^
      info.kgeorgiy.ja.belousov.bank.BankTests
