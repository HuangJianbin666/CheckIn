import { request } from './request'

export function register(username, password) {
  return request({
    url: '/auth/register',
    method: 'POST',
    data: { username, password },
  })
}

export function login(username, password) {
  return request({
    url: '/auth/login',
    method: 'POST',
    data: { username, password },
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'POST',
  })
}

export function me() {
  return request({
    url: '/auth/me',
    method: 'GET',
  })
}