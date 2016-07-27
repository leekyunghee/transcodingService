package leekyunghee.examples.tdd.transcodingService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import leekyunghee.examples.tdd.transcodingService.Job.State;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class TranscodingServiceTest {

	private Long jobId = new Long(1);
	
	@Mock
	private MediaSourceCopier mediaSourceCopier;
	@Mock
	private Transcoder transcoder;
	@Mock
	private CreatedFileSender createdFileSender;
	@Mock
	private ThumbnailExtractor thumbnailExtractor;
	@Mock
	private JobResultNotifier jobResultNotifier;
	
	// 테스트 클래스가 TranscodingService 클래스를 사용하도록 변경할 차례
	private TranscodingService transcodingService;
	@Mock
	private JobRepository jobRepository;	// 객체의 영속성을 담는곳(repository)
	@Mock
	private JobStateChanger jobStateChanger;
	@Mock
	private TranscodingExceptionHandler transcodingExceptionHandler;
	
	private Job mockJob = new Job();
	
	@Before
	public void setup() {
		transcodingService = new TranscodingServiceImpl(mediaSourceCopier, transcoder, 
				thumbnailExtractor, createdFileSender, jobResultNotifier, jobStateChanger, transcodingExceptionHandler);
		
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Job.State newState = (State) invocation.getArguments()[1];
				mockJob.changeState(newState);
				return null;
			}
		}).when(jobStateChanger).changeJobState(anyLong(), any(Job.State.class));
		
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				RuntimeException ex = (RuntimeException) invocation.getArguments()[1];
				mockJob.exceptionOccurred(ex);
				return null;
			}
		}).when(transcodingExceptionHandler).notifyToJob(anyLong(), any(RuntimeException.class));
	}

	@Test
	public void transcodeSuccessfully() {
		//when(jobRepository.findById(jobId)).thenReturn(new Job()); // 발생 후 리턴시켜주는 역할 
		when(jobRepository.findById(jobId)).thenReturn(mockJob);
		
		//Long jobId = new Long(1);
		File mockMultimediaFile = mock(File.class);
		when(mediaSourceCopier.copy(jobId)).thenReturn(mockMultimediaFile);
		
		List<File> mockMultimediaFiles = new ArrayList<File>();
		when(transcoder.transcode(mockMultimediaFile, jobId)).thenReturn(mockMultimediaFiles);
		
		List<File> mockThumbnails = new ArrayList<File>();
		when(thumbnailExtractor.extract(mockMultimediaFile, jobId)).thenReturn(mockThumbnails);
		
		transcodingService.transcode(jobId);
		
		Job job = jobRepository.findById(jobId);
		assertTrue(job.isFinished());
		assertTrue(job.isSuccess());
		assertEquals(Job.State.COMPLETED, job.getLastState());
		//assertEquals(Job.State.MEDIASOURCECOPYING, job.getLastState());
		assertNull(job.getOccurredException());
		
		verify(mediaSourceCopier, only()).copy(jobId);
		verify(transcoder, only()).transcode(mockMultimediaFile, jobId);
		verify(thumbnailExtractor, only()).extract(mockMultimediaFile, jobId);
		verify(createdFileSender, only()).store(mockMultimediaFiles, mockThumbnails, jobId);
		verify(jobResultNotifier, only()).notifyToRequester(jobId);
	}

	@Test 
	public void transcodingFailBecauseExceptionOccuredAtMediaSourceCopier() {
		//when(jobRepository.findById(jobId)).thenReturn(new Job());
		// Job객체를 mockJob으로 필드로 빼고 각 테스트 메서드에서 jobRepository Mock 객체의 findById(jobId) 메서드가 호출되면
		// mockJob을 리턴하도록 구현했다는 것이다. 
		when(jobRepository.findById(jobId)).thenReturn(mockJob);
		
		RuntimeException mockException = new RuntimeException();
		when(mediaSourceCopier.copy(jobId)).thenThrow(mockException);

		try {
			transcodingService.transcode(jobId);
			fail("발생해야함");
		}catch(Exception ex) {
			assertSame(mockException, ex);
		}

		Job job = jobRepository.findById(jobId);
		assertTrue(job.isFinished());
		assertFalse(job.isSuccess());
		assertEquals(Job.State.MEDIASOURCECOPYING, job.getLastState());
		assertNotNull(job.getOccurredException());
		
		verify(mediaSourceCopier, only()).copy(jobId);
		verify(transcoder, never()).transcode(any(File.class), anyLong());
		verify(thumbnailExtractor, never()).extract(any(File.class), anyLong());
		verify(createdFileSender, never()).store(anyListOf(File.class), anyListOf(File.class), anyLong());
		verify(jobResultNotifier, never()).notifyToRequester(jobId);
	}
	
	@Test 
	public void transcodingFailBecauseExceptionOccuredAtTranscode() {
		when(jobRepository.findById(jobId)).thenReturn(mockJob);
		
		RuntimeException mockException = new RuntimeException();
		when(transcoder.transcode(any(File.class), anyLong())).thenThrow(mockException);
		
		try {
			transcodingService.transcode(jobId);
			fail("발생해야함");
		}catch(Exception ex) {
			assertSame(mockException, ex);
		}
		
		Job job = jobRepository.findById(jobId);
		assertTrue(job.isFinished());
		assertFalse(job.isSuccess());
		assertEquals(Job.State.TRANSCODER, job.getLastState());
		assertNotNull(job.getOccurredException());
		
		verify(mediaSourceCopier, only()).copy(jobId);
		verify(transcoder, only()).transcode(any(File.class), anyLong());
		verify(thumbnailExtractor, never()).extract(any(File.class), anyLong());
		verify(createdFileSender, never()).store(anyListOf(File.class), anyListOf(File.class), anyLong());
		verify(jobResultNotifier, never()).notifyToRequester(jobId);
	}

	@Test
	public void transcodingFailBecauseExceptionOccuredAtExtractThumbnails() {
		when(jobRepository.findById(jobId)).thenReturn(mockJob);
		
		RuntimeException mockException = new RuntimeException();
		when(thumbnailExtractor.extract(any(File.class), anyLong())).thenThrow(mockException);
		
		try {
			transcodingService.transcode(jobId);
			fail("발생해야함");
		}catch(Exception ex) {
			assertSame(mockException, ex);
		}
		
		Job job = jobRepository.findById(jobId);
		assertTrue(job.isFinished());
		assertFalse(job.isSuccess());
		assertEquals(Job.State.THUMBNAILEXTRACTOR, job.getLastState());
		assertNotNull(job.getOccurredException());
		
		verify(mediaSourceCopier, only()).copy(jobId);
		verify(transcoder, only()).transcode(any(File.class), anyLong());
		verify(thumbnailExtractor, only()).extract(any(File.class), anyLong());
		verify(createdFileSender, never()).store(anyListOf(File.class), anyListOf(File.class), anyLong());
		verify(jobResultNotifier, never()).notifyToRequester(jobId);
	}
	
	@Test
	public void transcodingFailBecauseExceptionOccuredAtSendCreateFilesToDestination() {
		when(jobRepository.findById(jobId)).thenReturn(mockJob);
		
		RuntimeException mockException = new RuntimeException();
		// void 타입일 경우 doThrow 사용 
		doThrow(mockException).when(createdFileSender).store(anyListOf(File.class), anyListOf(File.class), anyLong());
		
		try {
			transcodingService.transcode(jobId);
			fail("발생해야함");
		}catch(Exception ex) {
			assertSame(mockException, ex);
		}
		
		Job job = jobRepository.findById(jobId);
		assertTrue(job.isFinished());
		assertFalse(job.isSuccess());
		assertEquals(Job.State.CREATEDFILESENDOR, job.getLastState());
		assertNotNull(job.getOccurredException());
		
		verify(mediaSourceCopier, only()).copy(jobId);
		verify(transcoder, only()).transcode(any(File.class), anyLong());
		verify(thumbnailExtractor, only()).extract(any(File.class), anyLong());
		verify(createdFileSender, only()).store(anyListOf(File.class), anyListOf(File.class), anyLong());
		verify(jobResultNotifier, never()).notifyToRequester(jobId);
	}
	
	@Test
	public void transcodingFailBecauseExceptionOccuredAtNotifyJobResultToRequester() {
		when(jobRepository.findById(jobId)).thenReturn(mockJob);
		
		RuntimeException mockException = new RuntimeException();
		doThrow(mockException).when(jobResultNotifier).notifyToRequester(anyLong());
		
		try {
			transcodingService.transcode(jobId);
			fail("발생해야함");
		}catch(Exception ex) {
			assertSame(mockException, ex);
		}
		
		Job job = jobRepository.findById(jobId);
		assertTrue(job.isFinished());
		assertFalse(job.isSuccess());
		assertEquals(Job.State.JOBRESULTNOTIFIER, job.getLastState());
		assertNotNull(job.getOccurredException());
		
		verify(mediaSourceCopier, only()).copy(jobId);
		verify(transcoder, only()).transcode(any(File.class), anyLong());
		verify(thumbnailExtractor, only()).extract(any(File.class), anyLong());
		verify(createdFileSender, only()).store(anyListOf(File.class), anyListOf(File.class), anyLong());
		verify(jobResultNotifier, only()).notifyToRequester(jobId);
	}
}
