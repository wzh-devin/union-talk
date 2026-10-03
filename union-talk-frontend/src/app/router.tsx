import { createBrowserRouter, Navigate, type RouteObject } from 'react-router'

import { AppShell } from '@/components/common/app-shell'
import { RequireAuth } from '@/app/require-auth'
import { AuthPage } from '@/pages/auth/auth-page'
import { HomePage } from '@/pages/home'
import { FriendsPage } from '@/pages/home/friends'
import { GroupsPage } from '@/pages/home/groups'
import { NotificationsPage } from '@/pages/home/notifications'
import { ProfilePage } from '@/pages/home/profile'
import {
  defaultHomeSection,
  homeRouteMap,
} from '@/pages/home/model/home-routes'
import { NotFoundPage } from '@/pages/not-found/not-found-page'

export const appRouteList: RouteObject[] = [
  {
    path: '/auth',
    element: <AuthPage />,
  },
  {
    element: <RequireAuth />,
    children: [
      {
        path: '/home',
        element: <HomePage />,
        children: [
          {
            index: true,
            element: <Navigate to={defaultHomeSection} replace />,
          },
          {
            path: 'groups',
            element: <GroupsPage />,
          },
          {
            path: 'friends',
            element: <FriendsPage />,
          },
          {
            path: 'notifications',
            element: <NotificationsPage />,
          },
          {
            path: 'profile',
            element: <ProfilePage />,
          },
        ],
      },
    ],
  },
  {
    path: '/',
    element: <AppShell />,
    children: [
      {
        index: true,
        element: <Navigate to={homeRouteMap[defaultHomeSection]} replace />,
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]

export const router = createBrowserRouter(appRouteList)
