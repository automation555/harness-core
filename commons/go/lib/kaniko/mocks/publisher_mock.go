// Code generated by MockGen. DO NOT EDIT.
// Source: publisher.go

// Package kaniko_mock is a generated GoMock package.
package kaniko_mock

import (
	gomock "github.com/golang/mock/gomock"
	reflect "reflect"
)

// MockRegistry is a mock of Registry interface.
type MockRegistry struct {
	ctrl     *gomock.Controller
	recorder *MockRegistryMockRecorder
}

// MockRegistryMockRecorder is the mock recorder for MockRegistry.
type MockRegistryMockRecorder struct {
	mock *MockRegistry
}

// NewMockRegistry creates a new mock instance.
func NewMockRegistry(ctrl *gomock.Controller) *MockRegistry {
	mock := &MockRegistry{ctrl: ctrl}
	mock.recorder = &MockRegistryMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockRegistry) EXPECT() *MockRegistryMockRecorder {
	return m.recorder
}

// Publish mocks base method.
func (m *MockRegistry) Publish(filename, fileContext, destination string) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Publish", filename, fileContext, destination)
	ret0, _ := ret[0].(error)
	return ret0
}

// Publish indicates an expected call of Publish.
func (mr *MockRegistryMockRecorder) Publish(filename, fileContext, destination interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Publish", reflect.TypeOf((*MockRegistry)(nil).Publish), filename, fileContext, destination)
}
