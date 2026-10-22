package net.shoreline.client.util.collection;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public final class FirstOutQueue<E> extends ForwardingQueue<E> implements Serializable {
   private final ArrayDeque<E> delegate;
   final int maxSize;

   public FirstOutQueue(int maxSize) {
      Preconditions.checkArgument(maxSize >= 0, "maxSize (%s) must >= 0", maxSize);
      this.delegate = new ArrayDeque<>(maxSize);
      this.maxSize = maxSize;
   }

   public int remainingCapacity() {
      return this.maxSize - this.size();
   }

   @NotNull
   protected ArrayDeque<E> delegate() {
      return this.delegate;
   }

   public boolean offer(E e) {
      return this.add(e);
   }

   public boolean add(E e) {
      Preconditions.checkNotNull(e);
      if (this.maxSize == 0) {
         return true;
      } else {
         if (this.size() == this.maxSize) {
            this.delegate.remove();
         }

         this.delegate.add(e);
         return true;
      }
   }

   public E addFirst(E e) {
      Preconditions.checkNotNull(e);
      if (this.maxSize == 0) {
         return null;
      } else {
         E removed = null;
         if (this.size() == this.maxSize) {
            removed = this.delegate.remove();
         }

         this.delegate.addFirst(e);
         return removed;
      }
   }

   public E getFirst() {
      return this.delegate.getFirst();
   }

   public E getLast() {
      return this.delegate.getLast();
   }

   public boolean addAll(Collection<? extends E> collection) {
      int size = collection.size();
      if (size >= this.maxSize) {
         this.clear();
         return Iterables.addAll(this, Iterables.skip(collection, size - this.maxSize));
      } else {
         return this.standardAddAll(collection);
      }
   }

   public Object[] toArray() {
      return super.toArray();
   }
}
