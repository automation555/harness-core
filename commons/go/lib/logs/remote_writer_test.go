package logs

import (
	"context"
	"testing"
	"time"

	gomock "github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/product/log-service/client"
	"github.com/wings-software/portal/product/log-service/mock"
)

func Test_GetRemoteWriter_Success(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mclient := mock.NewMockClient(ctrl)
	rw, _ := NewRemoteWriter(mclient, "key")
	assert.NotEqual(t, rw, nil)
}

func Test_RemoteWriter_Open_Success(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Open(context.Background(), "key").Return(nil)
	rw, _ := NewRemoteWriter(mclient, "key")
	err := rw.Open()
	assert.Equal(t, err, nil)
}

func Test_RemoteWriter_Open_Failure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	msg1 := "Write data 1\n"
	msg2 := "Write data 2\n"
	msg3 := "Write data 3\n"
	key := "key"
	strLink := "http://minio:9000"
	link := &client.Link{Value: strLink}

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Open(context.Background(), key).Return(errors.New("err"))
	mclient.EXPECT().UploadLink(context.Background(), key).Return(link, nil)
	mclient.EXPECT().UploadUsingLink(context.Background(), strLink, gomock.Any())
	mclient.EXPECT().Close(context.Background(), key)

	rw, _ := NewRemoteWriter(mclient, key)
	err := rw.Open()
	assert.NotEqual(t, err, nil)

	// Opening of the stream has failed but we can still use the writer
	rw.SetInterval(time.Duration(100) * time.Second)
	rw.Write([]byte(msg1))
	rw.Write([]byte(msg2))
	rw.Write([]byte(msg3))
	rw.flush() // Force write to the remote
	assert.Equal(t, len(rw.history), 3)

	err = rw.Close()
	assert.Nil(t, err)
}

// This test case needs to be modified if more fields are introduced in the line struct, since
// the limits might change. The limit should be set as size of line1 + size of line2 + delta
func Test_RemoteWriter_Limits(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	msg1 := "Write data 1\n"
	msg2 := "Write data 2\n"
	msg3 := "Write data 3\n"
	key := "key"

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Open(context.Background(), key).Return(nil)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())

	rw, _ := NewRemoteWriter(mclient, key)
	err := rw.Open()
	assert.Nil(t, err)

	// Opening of the stream has failed but we can still use the writer
	rw.SetInterval(time.Duration(100) * time.Second)
	rw.SetLimit(30)
	rw.Write([]byte(msg1))
	err = rw.flush()
	assert.Nil(t, err)
	rw.Write([]byte(msg2))
	err = rw.flush()
	assert.Nil(t, err)
	rw.Write([]byte(msg3))
	err = rw.flush()
	assert.Nil(t, err)

	assert.Equal(t, len(rw.history), 2)
	assert.Equal(t, len(rw.pending), 0)
	assert.Equal(t, rw.history[0].Message, msg2)
	assert.Equal(t, rw.history[1].Message, msg3)
}

func Test_RemoteWriter_WriteSingleLine(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	msg := "Write data 1\n"
	key := "key"

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	rw, _ := NewRemoteWriter(mclient, key)
	rw.SetInterval(time.Duration(100) * time.Second)
	rw.Write([]byte(msg))
	rw.flush() // Force write to the remote

	assert.Equal(t, len(rw.history), 1)
	assert.Equal(t, rw.history[0].Level, "info")
	assert.Equal(t, rw.history[0].Number, 0)
	assert.Equal(t, rw.history[0].Message, msg)
	assert.Equal(t, rw.history[0].Args, map[string]string{})
}

func Test_RemoteWriter_WriteMultiple(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	msg1 := "Write data 1\n"
	msg2 := "Write data 2\n"
	key := "key"

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	rw, _ := NewRemoteWriter(mclient, key)
	rw.SetInterval(time.Duration(100) * time.Second)
	rw.Write([]byte(msg1))
	rw.flush() // Force write to the remote
	rw.Write([]byte(msg2))
	rw.flush() // Force write to the remote
	assert.Equal(t, len(rw.history), 2)
	// Ensure strict ordering
	assert.Equal(t, rw.history[0].Level, "info")
	assert.Equal(t, rw.history[0].Number, 0)
	assert.Equal(t, rw.history[0].Message, msg1)
	assert.Equal(t, rw.history[0].Args, map[string]string{})

	assert.Equal(t, rw.history[1].Level, "info")
	assert.Equal(t, rw.history[1].Number, 1)
	assert.Equal(t, rw.history[1].Message, msg2)
	assert.Equal(t, rw.history[1].Args, map[string]string{})
}

func Test_RemoteWriter_MultipleCharacters(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	// Commands like `mvn run` flush single characters multiple times. We ensure here
	// that lines are still created only with \n

	msg1 := "Write data 1"
	msg2 := "Write data 2"

	key := "key"

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	rw, _ := NewRemoteWriter(mclient, key)
	rw.SetInterval(time.Duration(100) * time.Second)

	// Write character by character followed by new line
	for _, c := range msg1 {
		rw.Write([]byte(string(c)))
		rw.flush()
	}
	rw.Write([]byte("\n"))
	for _, c := range msg2 {
		rw.Write([]byte(string(c)))
		rw.flush()
	}
	rw.Write([]byte("\n"))
	rw.flush()

	assert.Equal(t, len(rw.history), 2)
	// Ensure strict ordering
	assert.Equal(t, rw.history[0].Level, "info")
	assert.Equal(t, rw.history[0].Number, 0)
	assert.Equal(t, rw.history[0].Message, msg1+"\n")
	assert.Equal(t, rw.history[0].Args, map[string]string{})

	assert.Equal(t, rw.history[1].Level, "info")
	assert.Equal(t, rw.history[1].Number, 1)
	assert.Equal(t, rw.history[1].Message, msg2+"\n")
	assert.Equal(t, rw.history[1].Args, map[string]string{})
}

func Test_RemoteWriter_JSON(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	msg1 := `{"level":"warn","msg":"Testing","k1":"v1","k2":"v2"}`
	msg1 = msg1 + "\n"

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	rw, _ := NewRemoteWriter(mclient, key)
	rw.SetInterval(time.Duration(100) * time.Second)

	rw.Write([]byte(msg1))
	rw.flush()

	assert.Equal(t, len(rw.history), 1)
	// Ensure strict ordering
	assert.Equal(t, rw.history[0].Level, "warn")
	assert.Equal(t, rw.history[0].Number, 0)
	assert.Equal(t, rw.history[0].Message, "Testing")
	assert.Equal(t, rw.history[0].Args, map[string]string{"level": "warn", "msg": "Testing", "k1": "v1", "k2": "v2"})
}

func Test_RemoteWriter_Close(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	msg1 := `{"level":"warn","msg":"Testing","k1":"v1","k2":"v2"}`
	strLink := "http://minio:9000"
	link := &client.Link{Value: strLink}
	msg1 = msg1 + "\n"
	msg2 := "Another message" // Ensure this gets flushed on close

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	mclient.EXPECT().UploadLink(context.Background(), key).Return(link, nil)
	mclient.EXPECT().UploadUsingLink(context.Background(), strLink, gomock.Any())
	mclient.EXPECT().Close(context.Background(), key)
	rw, _ := NewRemoteWriter(mclient, key)
	rw.SetInterval(time.Duration(100) * time.Second)

	rw.Write([]byte(msg1))
	rw.flush()

	rw.Write([]byte(msg2))
	rw.flush()

	assert.Equal(t, rw.prev, []byte(msg2))

	assert.Equal(t, len(rw.history), 1)
	// Ensure strict ordering
	assert.Equal(t, rw.history[0].Level, "warn")
	assert.Equal(t, rw.history[0].Number, 0)
	assert.Equal(t, rw.history[0].Message, "Testing")
	assert.Equal(t, rw.history[0].Args, map[string]string{"level": "warn", "msg": "Testing", "k1": "v1", "k2": "v2"})

	rw.Close()
	assert.Equal(t, rw.prev, []byte{}) // Ensure existing data gets flushed
	assert.Equal(t, rw.closed, true)
}
