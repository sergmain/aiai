/*
 * Metaheuristic, Copyright (C) 2017-2021, Innovation platforms, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ai.metaheuristic.ai.dispatcher.exec_context_variable_state;

import ai.metaheuristic.ai.dispatcher.beans.ExecContextImpl;
import ai.metaheuristic.ai.dispatcher.beans.ExecContextTaskState;
import ai.metaheuristic.ai.dispatcher.beans.ExecContextVariableState;
import ai.metaheuristic.ai.dispatcher.commons.CommonSync;
import ai.metaheuristic.ai.utils.TxUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * it's a Spring based implementation because we need singleton
 *
 * @author Serge
 * Date: 8/11/2019
 * Time: 10:56 AM
 */
@Service
@RequiredArgsConstructor
@Profile("dispatcher")
@Slf4j
public class ExecContextVariableStateSyncService {

    private static final CommonSync<Long> commonSync = new CommonSync<>();

    public void checkWriteLockPresent(Long execContextVariableStateId) {
        if (!getWriteLock(execContextVariableStateId).isHeldByCurrentThread()) {
            throw new IllegalStateException("#976.020 Must be locked by WriteLock");
        }
    }

    public void checkWriteLockNotPresent(Long execContextVariableStateId) {
        if (getWriteLock(execContextVariableStateId).isHeldByCurrentThread()) {
            throw new IllegalStateException("#976.025 The thread was already locked by WriteLock");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public ReentrantReadWriteLock.WriteLock getWriteLock(Long execContextVariableStateId) {
        return commonSync.getWriteLock(execContextVariableStateId);
    }

    private ReentrantReadWriteLock.ReadLock getReadLock(Long execContextVariableStateId) {
        return commonSync.getReadLock(execContextVariableStateId);
    }

    public <T> T getWithSync(Long execContextVariableStateId, Supplier<T> supplier) {
        TxUtils.checkTxNotExists();
        checkWriteLockNotPresent(execContextVariableStateId);

        final ReentrantReadWriteLock.WriteLock lock = getWriteLock(execContextVariableStateId);
        try {
            lock.lock();
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    // ForCreation means that the presence of TX won't be checked
    @Nullable
    public <T> T getWithSyncNullableForCreation(Long execContextVariableStateId, Supplier<T> supplier) {
        checkWriteLockNotPresent(execContextVariableStateId);

        final ReentrantReadWriteLock.WriteLock lock = getWriteLock(execContextVariableStateId);
        try {
            lock.lock();
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    public <T> T getWithSyncNullable(Long execContextVariableStateId, Supplier<T> supplier) {
        TxUtils.checkTxNotExists();
        checkWriteLockNotPresent(execContextVariableStateId);

        final ReentrantReadWriteLock.WriteLock lock = getWriteLock(execContextVariableStateId);
        try {
            lock.lock();
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    public <T> T getWithSyncReadOnly(ExecContextVariableState execContextVariableState, Supplier<T> supplier) {
        TxUtils.checkTxNotExists();
        checkWriteLockNotPresent(execContextVariableState.id);

        final ReentrantReadWriteLock.ReadLock lock = getReadLock(execContextVariableState.id);
        try {
            lock.lock();
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }
}
