package nl.han.ica.datastructures;

import java.util.ArrayList;
import java.util.List;

public class HANStack<T> {
    private final List<T> data = new ArrayList<>();

    /**
     * pushes value T to the top of the stack
     * @param value value to push
     */
    public void push(T value){
     data.add(value);
    }

    /**
     * Pops (and removes) value at top of stack
     * @return popped value
     */
    public T pop(){
        if (data.isEmpty()){
            throw new RuntimeException("Stack is empty");
        }
        return data.remove(data.size() - 1);
    }

    /**
     * Peeks at the top of the stack. Does not remove anything
     * @return value at the top of the stack
     */
    public T peek(){
        if (data.isEmpty()){
            throw new RuntimeException("Stack is empty");
        }
        return data.get(data.size() - 1);
    }

    public boolean isEmpty(){
        return data.isEmpty();
    }
    public int size(){
        return data.size();
    }
    public void clear(){
        data.clear();
    }
}
