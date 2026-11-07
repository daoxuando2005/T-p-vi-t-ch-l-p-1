import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';

// Auth Pages
import Login from './pages/Auth/Login';
import Register from './pages/Auth/Register';

// Student Pages
import StudentDashboard from './pages/Student/Dashboard';
import Alphabet from './pages/Student/Alphabet';
import WritingCanvas from './pages/Student/WritingCanvas';
import StudentClasses from './pages/Student/Classes';

// Teacher Pages
import TeacherDashboard from './pages/Teacher/Dashboard';
import TeacherAssignments from './pages/Teacher/Assignments';
import TeacherAlphabetManagement from './pages/Teacher/AlphabetManagement';

// Parent Pages
import ParentDashboard from './pages/Parent/Dashboard';
import ParentAssignmentManagement from './pages/Parent/AssignmentManagement';

// Admin Pages
import AdminDashboard from './pages/Admin/Dashboard';

// Chat Realtime Page
import Chat from './pages/Chat';

const App: React.FC = () => {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Student Protected Routes */}
          <Route path="/student" element={
            <ProtectedRoute allowedRole="STUDENT">
              <Layout>
                <StudentDashboard />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/student/alphabet" element={
            <ProtectedRoute allowedRole="STUDENT">
              <Layout>
                <Alphabet />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/student/practice" element={
            <ProtectedRoute allowedRole="STUDENT">
              <Layout>
                <WritingCanvas />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/student/classes" element={
            <ProtectedRoute allowedRole="STUDENT">
              <Layout>
                <StudentClasses />
              </Layout>
            </ProtectedRoute>
          } />


          {/* Teacher Protected Routes */}
          <Route path="/teacher" element={
            <ProtectedRoute allowedRole="TEACHER">
              <Layout>
                <TeacherDashboard />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/teacher/classes" element={
            <ProtectedRoute allowedRole="TEACHER">
              <Layout>
                <TeacherDashboard />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/teacher/assignments" element={
            <ProtectedRoute allowedRole="TEACHER">
              <Layout>
                <TeacherAssignments />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/teacher/alphabet" element={
            <ProtectedRoute allowedRole="TEACHER">
              <Layout>
                <TeacherAlphabetManagement />
              </Layout>
            </ProtectedRoute>
          } />

          {/* Parent Protected Routes */}
          <Route path="/parent" element={
            <ProtectedRoute allowedRole="PARENT">
              <Layout>
                <ParentDashboard />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/parent/link" element={
            <ProtectedRoute allowedRole="PARENT">
              <Layout>
                <ParentDashboard />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/parent/assignments" element={
            <ProtectedRoute allowedRole="PARENT">
              <Layout>
                <ParentAssignmentManagement />
              </Layout>
            </ProtectedRoute>
          } />

          {/* Admin Protected Routes */}
          <Route path="/admin" element={
            <ProtectedRoute allowedRole="ADMIN">
              <Layout>
                <AdminDashboard />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/admin/users" element={
            <ProtectedRoute allowedRole="ADMIN">
              <Layout>
                <AdminDashboard />
              </Layout>
            </ProtectedRoute>
          } />
          <Route path="/admin/lessons" element={
            <ProtectedRoute allowedRole="ADMIN">
              <Layout>
                <AdminDashboard />
              </Layout>
            </ProtectedRoute>
          } />

          {/* Realtime Chat Route (accessible by both Parents & Teachers) */}
          <Route path="/chat" element={
            <ProtectedRoute>
              <Layout>
                <Chat />
              </Layout>
            </ProtectedRoute>
          } />

          {/* Fallback Redirect */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
};


export default App;
