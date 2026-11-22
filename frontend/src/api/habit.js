import { request } from './request'

export function createHabit({ name, durationMinutes = null }) {
  return request({
    url: '/habits',
    method: 'POST',
    data: { name, durationMinutes },
  })
}

export function updateHabit(habitId, { name, durationMinutes }) {
  return request({
    url: `/habits/${habitId}`,
    method: 'PUT',
    data: { name, durationMinutes },
  })
}

export function deleteHabit(habitId) {
  return request({
    url: `/habits/${habitId}`,
    method: 'DELETE',
  })
}

export function listHabits() {
  return request({
    url: '/habits',
    method: 'GET',
  })
}

export function checkIn(habitId) {
  return request({
    url: `/habits/${habitId}/check-ins`,
    method: 'POST',
  })
}

export function getHabitDetail(habitId) {
  return request({
    url: `/habits/${habitId}`,
    method: 'GET',
  })
}