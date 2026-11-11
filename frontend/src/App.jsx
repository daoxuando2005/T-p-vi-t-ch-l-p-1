import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
// Auth Pages
import Login from './pages/Auth/Login';
import Register from './pages/Auth/Register';
// Teacher Pages
import TeacherDashboard from './pages/Teacher/Dashboard';
import TeacherAssignments from './pages/Teacher/Assignments';
import TeacherAlphabetManagement from './pages/Teacher/AlphabetManagement';
// Admin Pages
import AdminDashboard from './pages/Admin/Dashboard';
const App = () => {
    return (<Router>
      <AuthProvider>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />}/>
          <Route path="/register" element={<Register />}/>

          {/* Teacher Protected Routes */}
          <Route path="/teacher" element={<ProtectedRoute allowedRole="TEACHER">
              <Layout>
                <TeacherDashboard />
              </Layout>
            </ProtectedRoute>}/>
          <Route path="/teacher/classes" element={<ProtectedRoute allowedRole="TEACHER">
              <Layout>
                <TeacherDashboard />
              </Layout>
            </ProtectedRoute>}/>
          <Route path="/teacher/assignments" element={<ProtectedRoute allowedRole="TEACHER">
              <Layout>
                <TeacherAssignments />
              </Layout>
            </ProtectedRoute>}/>
          <Route path="/teacher/alphabet" element={<ProtectedRoute allowedRole="TEACHER">
              <Layout>
                <TeacherAlphabetManagement />
              </Layout>
            </ProtectedRoute>}/>

          {/* Admin Protected Routes */}
          <Route path="/admin" element={<ProtectedRoute allowedRole="ADMIN">
              <Layout>
                <AdminDashboard />
              </Layout>
            </ProtectedRoute>}/>
          <Route path="/admin/users" element={<ProtectedRoute allowedRole="ADMIN">
              <Layout>
                <AdminDashboard />
              </Layout>
            </ProtectedRoute>}/>
          <Route path="/admin/lessons" element={<ProtectedRoute allowedRole="ADMIN">
              <Layout>
                <AdminDashboard />
              </Layout>
            </ProtectedRoute>}/>

          {/* Fallback Redirect */}
          <Route path="*" element={<Navigate to="/login" replace/>}/>
        </Routes>
      </AuthProvider>
    </Router>);
};
export default App;
