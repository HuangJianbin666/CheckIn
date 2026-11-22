import { request } from './request'

export function createHabit(name) {
  return request({
    url: '/habits',
    method: 'POST',
    data: { name },
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