/*
 * Copyright (C) 2017 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package org.isoron.uhabits.core.models.sqlite;

import org.isoron.uhabits.*;
import org.isoron.uhabits.core.database.*;
import org.isoron.uhabits.core.models.*;
import org.isoron.uhabits.core.models.sqlite.records.*;
import org.junit.*;
import org.junit.rules.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.core.IsEqual.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SQLiteHabitListTest extends BaseUnitTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private SQLiteHabitList habitList;

    private Repository<HabitRecord> repository;

    private ModelObservable.Listener listener;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        Database db = buildMemoryDatabase();
        repository = new Repository<>(HabitRecord.class, db);
        habitList = new SQLiteHabitList(new SQLModelFactory(db));

        for (int i = 0; i < 10; i++)
        {
            Habit h = modelFactory.buildHabit();
            h.setName("habit " + i);
            h.setId((long) i);
            if (i % 2 == 0) h.setArchived(true);

            HabitRecord record = new HabitRecord();
            record.copyFrom(h);
            record.position = i;
            repository.save(record);
        }

        habitList.reload();

        listener = mock(ModelObservable.Listener.class);
        habitList.getObservable().addListener(listener);
    }

    @Override
    public void tearDown() throws Exception
    {
        habitList.getObservable().removeListener(listener);
        super.tearDown();
    }

    @Test
    public void testAdd_withDuplicate()
    {
        Habit habit = modelFactory.buildHabit();
        habitList.add(habit);
        verify(listener).onModelChange();

        exception.expect(IllegalArgumentException.class);
        habitList.add(habit);
    }

    @Test
    public void testAdd_withId()
    {
        Habit habit = modelFactory.buildHabit();
        habit.setName("Hello world with id");
        habit.setId(12300L);

        habitList.add(habit);
        assertThat(habit.getId(), equalTo(12300L));

        HabitRecord record = repository.find(12300L);
        assertNotNull(record);
        assertThat(record.name, equalTo(habit.getName()));
    }

    @Test
    public void testAdd_withoutId()
    {
        Habit habit = modelFactory.buildHabit();
        habit.setName("Hello world");
        assertNull(habit.getId());

        habitList.add(habit);
        assertNotNull(habit.getId());

        HabitRecord record = repository.find(habit.getId());
        assertNotNull(record);
        assertThat(record.name, equalTo(habit.getName()));
    }

    @Test
    public void testSize()
    {
        assertThat(habitList.size(), equalTo(10));
    }

    @Test
    public void testGetById()
    {
        Habit h1 = habitList.getById(0);
        assertNotNull(h1);
        assertThat(h1.getName(), equalTo("habit 0"));

        Habit h2 = habitList.getById(0);
        assertNotNull(h2);
        assertThat(h1, equalTo(h2));
    }

    @Test
    public void testGetById_withInvalid()
    {
        long invalidId = 9183792001L;
        Habit h1 = habitList.getById(invalidId);
        assertNull(h1);
    }

    @Test
    public void testGetByPosition()
    {
        Habit h = habitList.getByPosition(5);
        assertNotNull(h);
        assertThat(h.getName(), equalTo("habit 5"));
    }

    @Test
    public void testIndexOf()
    {
        Habit h1 = habitList.getByPosition(5);
        assertNotNull(h1);
        assertThat(habitList.indexOf(h1), equalTo(5));

        Habit h2 = modelFactory.buildHabit();
        assertThat(habitList.indexOf(h2), equalTo(-1));

        h2.setId(1000L);
        assertThat(habitList.indexOf(h2), equalTo(-1));
    }
}