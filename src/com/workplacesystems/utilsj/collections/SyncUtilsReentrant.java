/*
 * Copyright 2010 Workplace Systems PLC (http://www.workplacesystems.com/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.workplacesystems.utilsj.collections;

import com.workplacesystems.utilsj.Callback;
import com.workplacesystems.utilsj.Condition;
import com.workplacesystems.utilsj.UtilsjException;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * IMPORTANT: This source must be kept in sync with SyncUtilsJdk16
 *
 * @author dave
 */
class SyncUtilsReentrant extends SyncUtilsLegacy
{
    /** 
     * Time allowed for getting each lock in a {@link SyncListReentrant}.  Apart from unit tests,
     * no client code should alter this value.
     */
    static int LOCK_TIMEOUT_SECONDS = 30;

    SyncUtilsReentrant()
    {
    }

    @Override
    Object createMutexImpl(Object suggested_mutex)
    {
        if (suggested_mutex instanceof ReentrantReadWriteLock)
            return suggested_mutex;

        suggested_mutex = getObjectToLock(suggested_mutex);
        if (suggested_mutex instanceof ReentrantReadWriteLock)
            return suggested_mutex;

        return new ReentrantReadWriteLock();
    }

    public static class SyncListReentrant extends SyncList
    {
        @Override
        boolean tryLock(Object mutex)
        {
            if (mutex instanceof ReentrantReadWriteLock)
            {
                ReentrantReadWriteLock lock = (ReentrantReadWriteLock)mutex;

                if (lock.getReadHoldCount() > 0 && lock.getWriteHoldCount() == 0)
                    throw new IllegalStateException("Lock cannot be upgraded from read to write");

                try
                {
                    return lock.writeLock().tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
                catch (InterruptedException e)
                {
                    return false;
                }
            }
            else
                throw new IllegalStateException("Lock cannot be locked within SyncList");
        }

        @Override
        void unlock(Object mutex)
        {
            ReentrantReadWriteLock lock = (ReentrantReadWriteLock)mutex;
            lock.writeLock().unlock();
        }
        
    }

    @Override
    SyncList getNewSyncListImpl()
    {
        return new SyncListReentrant();
    }

    @Override
    <T> T  synchronizeWriteImpl(Object mutex, Callback<T> callback, Callback<?> release_callback)
    {
        if (mutex instanceof ReentrantReadWriteLock)
        {
            ReentrantReadWriteLock lock = (ReentrantReadWriteLock)mutex;

            if (lock.getReadHoldCount() > 0 && lock.getWriteHoldCount() == 0)
                throw new IllegalStateException("Lock cannot be upgraded from read to write");

            lock.writeLock().lock();
            try
            {
                return callback.action();
            }
            finally
            {
                try
                {
                    if (lock.getReadHoldCount() == 0 && lock.getWriteHoldCount() == 1 && release_callback != null)
                        release_callback.action();
                }
                finally
                {
                    lock.writeLock().unlock();
                }
            }
        }
        else if (mutex instanceof SyncList)
        {
            if (release_callback != null)
                throw new UtilsjException("release_callback not supported for SyncLists");

            SyncList sync_list = (SyncList)mutex;
            try
            {
                sync_list.lockAll();
                return callback.action();
            }
            finally
            {
                sync_list.unlockAll();
            }
        }
        else
            return super.synchronizeWriteImpl(mutex, callback, release_callback);
    }

    @Override
    <T> T  synchronizeReadImpl(Object mutex, Callback<T> callback, Callback<?> release_callback)
    {
        if (mutex instanceof ReentrantReadWriteLock)
        {
            ReentrantReadWriteLock lock = (ReentrantReadWriteLock)mutex;

            lock.readLock().lock();
            try
            {
                return callback.action();
            }
            finally
            {
                try
                {
                    if (lock.getReadHoldCount() == 1 && lock.getWriteHoldCount() == 0 && release_callback != null)
                        release_callback.action();
                }
                finally
                {
                    lock.readLock().unlock();
                }
            }
        }
        else if (mutex instanceof SyncList)
        {
            throw new UnsupportedOperationException("SyncList cannot be locked for read.");
        }
        else
            return super.synchronizeReadImpl(mutex, callback, release_callback);
    }

    @Override
    <T> T  synchronizeWriteThenReadImpl(Object mutex, Callback<?> write_callback, Callback<T> read_callback, Callback<?> release_callback)
    {
        if (mutex instanceof ReentrantReadWriteLock)
        {
            ReentrantReadWriteLock lock = (ReentrantReadWriteLock)mutex;

            boolean read_lock = false;
            boolean write_lock = false;

            if (lock.getReadHoldCount() > 0 && lock.getWriteHoldCount() == 0)
                throw new IllegalStateException("Lock cannot be upgraded from read to write");

            lock.writeLock().lock();
            write_lock = true;

            lock.readLock().lock();
            read_lock = true;
            
            try
            {
                write_callback.action();

                lock.writeLock().unlock();
                write_lock = false;
                
                return read_callback.action();
            }
            finally
            {
                if (write_lock)
                {
                    try
                    {
                        if (!read_lock)
                        {
                            if (lock.getReadHoldCount() == 0 && lock.getWriteHoldCount() == 1 && release_callback != null)
                                release_callback.action();
                        }
                    }
                    finally
                    {
                        lock.writeLock().unlock();
                    }
                }

                if (read_lock)
                {
                    try
                    {
                        if (lock.getReadHoldCount() == 1 && lock.getWriteHoldCount() == 0 && release_callback != null)
                            release_callback.action();
                    }
                    finally
                    {
                        lock.readLock().unlock();
                    }
                }
            }
        }
        else if (mutex instanceof SyncList)
        {
            throw new UnsupportedOperationException("SyncList cannot be locked for write then read.");
        }
        else
            return super.synchronizeWriteThenReadImpl(mutex, write_callback, read_callback, release_callback);
    }

    @Override
    <T> T  synchronizeConditionalWriteImpl(Object mutex, Condition write_condition, Callback<T> write_callback, Callback<?> release_callback)
    {
        if (mutex instanceof ReentrantReadWriteLock)
        {
            ReentrantReadWriteLock lock = (ReentrantReadWriteLock)mutex;

            boolean read_lock = false;
            boolean write_lock = false;

            int read_hold_count = lock.getReadHoldCount();
            if (read_hold_count > 0 && write_condition.isTrue(read_hold_count))
                throw new IllegalStateException("Lock cannot be upgraded from read to write");

            // First take the read lock
            lock.readLock().lock();
            read_lock = true;

            try
            {
                // Check whether the write condition is true
                if (write_condition.isTrue(read_hold_count))
                {
                    lock.readLock().unlock();
                    read_lock = false;

                    // At this point there is no lock and therefore the write condition maybe altered
                    lock.writeLock().lock();
                    write_lock = true;

                    // So check again now we have the write lock
                    if (write_condition.isTrue(read_hold_count))
                    {
                        write_callback.action();

                        lock.writeLock().unlock();
                        write_lock = false;
                    }
                }

                return null;
            }
            finally
            {
                if (write_lock)
                {
                    try
                    {
                        if (!read_lock)
                        {
                            if (lock.getReadHoldCount() == 0 && lock.getWriteHoldCount() == 1 && release_callback != null)
                                release_callback.action();
                        }
                    }
                    finally
                    {
                        lock.writeLock().unlock();
                    }
                }

                if (read_lock)
                {
                    try
                    {
                        if (lock.getReadHoldCount() == 1 && lock.getWriteHoldCount() == 0 && release_callback != null)
                            release_callback.action();
                    }
                    finally
                    {
                        lock.readLock().unlock();
                    }
                }
            }
        }
        else if (mutex instanceof SyncList)
        {
            throw new UnsupportedOperationException("SyncList cannot be locked for conditional write.");
        }
        else
            return super.synchronizeConditionalWriteImpl(mutex, write_condition, write_callback, release_callback);
    }

    @Override
    <T> T  synchronizeConditionalWriteThenReadImpl(Object mutex, Condition write_condition, Callback<?> write_callback, Callback<T> read_callback, Callback<?> release_callback)
    {
        if (mutex instanceof ReentrantReadWriteLock)
        {
            ReentrantReadWriteLock lock = (ReentrantReadWriteLock)mutex;

            boolean read_lock = false;
            boolean write_lock = false;

            int read_hold_count = lock.getReadHoldCount();
            if (read_hold_count > 0 && write_condition.isTrue(read_hold_count))
                throw new IllegalStateException("Lock cannot be upgraded from read to write");

            // First take the read lock
            lock.readLock().lock();
            read_lock = true;

            try
            {
                // Check whether the write condition is true
                if (write_condition.isTrue(read_hold_count))
                {
                    lock.readLock().unlock();
                    read_lock = false;

                    // At this point there is no lock and therefore the write condition maybe altered
                    lock.writeLock().lock();
                    write_lock = true;

                    // Retake the read lock now we have the write lock
                    lock.readLock().lock();
                    read_lock = true;

                    // So check again now we have the write lock
                    if (write_condition.isTrue(read_hold_count))
                    {
                        write_callback.action();

                        lock.writeLock().unlock();
                        write_lock = false;
                    }
                }

                return read_callback.action();
            }
            finally
            {
                if (write_lock)
                {
                    try
                    {
                        if (!read_lock)
                        {
                            if (lock.getReadHoldCount() == 0 && lock.getWriteHoldCount() == 1 && release_callback != null)
                                release_callback.action();
                        }
                    }
                    finally
                    {
                        lock.writeLock().unlock();
                    }
                }

                if (read_lock)
                {
                    try
                    {
                        if (lock.getReadHoldCount() == 1 && lock.getWriteHoldCount() == 0 && release_callback != null)
                            release_callback.action();
                    }
                    finally
                    {
                        lock.readLock().unlock();
                    }
                }
            }
        }
        else if (mutex instanceof SyncList)
        {
            throw new UnsupportedOperationException("SyncList cannot be locked for conditional write then read.");
        }
        else
            return super.synchronizeConditionalWriteThenReadImpl(mutex, write_condition, write_callback, read_callback, release_callback);
    }
}
