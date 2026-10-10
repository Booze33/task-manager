import type { Task } from '../types'

const habits = [
  { name: 'Daily reading', title: 'Read for 20 minutes', description: 'Make space for a few pages of focused reading.' },
  { name: 'Morning movement', title: 'Morning stretch', description: 'Start the day with a short mobility session.' },
  { name: 'Deep work', title: 'Focused work block', description: 'Protect one block of distraction-free work.' },
]
const priorities: Task['priority'][] = ['medium', 'low', 'high', 'medium', 'low', 'medium']

export const createSeedTasks = (): Task[] => Array.from({ length: 47 }, (_, index) => {
  const habit = habits[index % habits.length]
  const dayOffset = index % 9
  const date = new Date()
  date.setDate(date.getDate() + dayOffset - 3)
  return {
    id: `task-${index + 1}`,
    title: index < habits.length ? habit.title : `${habit.title} · day ${Math.floor(index / habits.length) + 1}`,
    description: habit.description,
    status: index < 13 ? 'done' : index % 7 === 0 ? 'in-progress' : 'todo',
    habit: habit.name,
    dueDate: date.toISOString().slice(0, 10),
    createdAt: new Date(Date.now() - index * 86400000).toISOString(),
    priority: priorities[index % priorities.length],
  }
})
