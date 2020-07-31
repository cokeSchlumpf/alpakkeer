# Deployment

``` python hl_lines="2 3"
def bubble_sort(items):
    for i in range(len(items)):
        for j in range(len(items) - 1 - i):
            if items[j] > items[j + 1]:
                items[j], items[j + 1] = items[j + 1], items[j]
```

=== "C"

    ``` c
    #include <stdio.h>
    
    int main(void) {
        printf("Hello world!\n");
        return 0;
    }
    ```

=== "C++"

    ``` c++
    #include <iostream>
    _
    int main(void) {
      std::cout << "Hello world!" << std::endl;
      return 0;
    }
    ```