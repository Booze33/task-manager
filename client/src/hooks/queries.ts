import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/service'
import { PAGE_SIZE } from '../config'
import { useAppStore } from '../store'
import type { TaskStatus } from '../types'

export const useTasks = (page: number, status: TaskStatus | 'all', habit: string) =>
  useQuery({ queryKey: ['tasks', page, status, habit], queryFn: () => api.tasks(page, PAGE_SIZE, status, habit) })

export const useStats = () => useQuery({ queryKey: ['stats'], queryFn: api.stats })

export const useCompleteTask = () => {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ id, flakyRetry }: { id: string; flakyRetry: boolean }) => api.completeTask(id, flakyRetry),
    onSuccess: async () => {
      await Promise.all([client.invalidateQueries({ queryKey: ['tasks'] }), client.invalidateQueries({ queryKey: ['stats'] })])
      useAppStore.getState().showToast('Task completed. Nice work.')
    },
  })
}

export const useCreateTask = () => {
  const client = useQueryClient()
  return useMutation({
    mutationFn: api.createTask,
    onSuccess: async () => {
      await Promise.all([client.invalidateQueries({ queryKey: ['tasks'] }), client.invalidateQueries({ queryKey: ['stats'] })])
      useAppStore.getState().showToast('Task added to your list.')
    },
  })
}

export const useInvalidateStats = () => {
  const client = useQueryClient()
  return useMutation({
    mutationFn: api.invalidateStats,
    onSuccess: async () => {
      await client.refetchQueries({ queryKey: ['stats'] })
      useAppStore.getState().showToast('Stats cache invalidated.')
    },
  })
}
