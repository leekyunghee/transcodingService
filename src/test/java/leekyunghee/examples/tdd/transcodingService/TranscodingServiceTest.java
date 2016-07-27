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
	// 각각의 테스트 메서드에서 공통으로 사용되는 로컬 변수를 필드로 전환.
	private File mockMultimediaFile = mock(File.class);
	private List<File> mockMultimediaFiles = new ArrayList<File>();
	private List<File> mockThumbnails = new ArrayList<File>();
	
	// 중복코드 mockException 로컬 변수를 필드로 전환 
	private RuntimeException mockException = new RuntimeException();
	
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

		//when(jobRepository.findById(jobId)).thenReturn(new Job()); // 발생 후 리턴시켜주는 역할 
		when(jobRepository.findById(jobId)).thenReturn(mockJob);
		
		//Long jobId = new Long(1);
		// 필드로 바뀐 로컬 변수 선언 삭제됨. 
		when(mediaSourceCopier.copy(jobId)).thenReturn(mockMultimediaFile);
		
		when(transcoder.transcode(mockMultimediaFile, jobId)).thenReturn(mockMultimediaFiles);
		
		when(thumbnailExtractor.extract(mockMultimediaFile, jobId)).thenReturn(mockThumbnails);		
		
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
		// 필드로 바뀐 로컬 변수 선언 삭제됨. 

		Job job = jobRepository.findById(jobId);
		assertJobIsWaitingState();
		
		transcodingService.transcode(jobId);
		
		job = jobRepository.findById(jobId);
		assertTrue(job.isFinished());
		assertTrue(job.isSuccess());
		assertEquals(Job.State.COMPLETED, job.getLastState());
		assertNull(job.getOccurredException());
		
		VerifyOption verifyOption = new VerifyOption();
		verifyCollaboration(verifyOption);
		// 협업 객체가 호출되는지의 여부를 확인하는 verify 중복 제거 		
	}
	
	private void verifyCollaboration(VerifyOption verifyOption) {
		verify(mediaSourceCopier, only()).copy(jobId);
		
		if(verifyOption.transcoderNever) {
			verify(transcoder, never()).transcode(any(File.class), anyLong());
		}else {
			verify(transcoder, only()).transcode(any(File.class), anyLong());
		}
		
		if(verifyOption.thumbnailExtractorNever) {
			verify(thumbnailExtractor, never()).extract(any(File.class), anyLong());
		}else {
			verify(thumbnailExtractor, only()).extract(any(File.class), anyLong());
		}
		
		if(verifyOption.createdFileSenderNever) {
			verify(createdFileSender, never()).store(anyListOf(File.class), anyListOf(File.class), anyLong());
		}else {
			verify(createdFileSender, only()).store(anyListOf(File.class), anyListOf(File.class), anyLong());
		}
		
		if(verifyOption.jobResultNotifierNever) {
			verify(jobResultNotifier, never()).notifyToRequester(anyLong());			
		}else {
			verify(jobResultNotifier, only()).notifyToRequester(anyLong());
		}
	}

	private void assertJobIsWaitingState() {
		Job job = jobRepository.findById(jobId);
		assertTrue(job.isWaiting());
	}
	
	@Test 
	public void transcodingFailBecauseExceptionOccuredAtMediaSourceCopier() {
		// Job객체를 mockJob으로 필드로 빼고 각 테스트 메서드에서 jobRepository Mock 객체의 findById(jobId) 메서드가 호출되면
		// mockJob을 리턴하도록 구현했다는 것이다. 
		// 초기화 코드 제거 

		when(mediaSourceCopier.copy(jobId)).thenThrow(mockException);
		assertJobIsWaitingState();
		excuteFailingTranscodeAndAssertFail(Job.State.MEDIASOURCECOPYING);

        VerifyOption verifyOption = new VerifyOption();
        verifyOption.transcoderNever = true;
        verifyOption.thumbnailExtractorNever = true;
        verifyOption.createdFileSenderNever = true;
        verifyOption.jobResultNotifierNever = true;

        verifyCollaboration(verifyOption);
		
	}
	
	private void excuteFailingTranscodeAndAssertFail(State expectedLastState) {
		try {
			transcodingService.transcode(jobId);
			fail("발생해야함");
		}catch(Exception ex) {
			assertSame(mockException, ex);
		}
		
		Job job = jobRepository.findById(jobId);
		assertTrue(job.isFinished());
		assertFalse(job.isSuccess());
		assertEquals(expectedLastState, job.getLastState());
		assertNotNull(job.getOccurredException());
	}
	
	@Test 
	public void transcodingFailBecauseExceptionOccuredAtTranscode() {

		when(transcoder.transcode(any(File.class), anyLong())).thenThrow(mockException);
		assertJobIsWaitingState();
		excuteFailingTranscodeAndAssertFail(Job.State.TRANSCODER);
		
		VerifyOption verifyOption = new VerifyOption();
		verifyOption.transcoderNever = false;
	    verifyOption.thumbnailExtractorNever = true;
	    verifyOption.createdFileSenderNever = true;
	    verifyOption.jobResultNotifierNever = true;

	    verifyCollaboration(verifyOption);	
	}

	@Test
	public void transcodingFailBecauseExceptionOccuredAtExtractThumbnails() {

		when(thumbnailExtractor.extract(any(File.class), anyLong())).thenThrow(mockException);
		assertJobIsWaitingState();
		excuteFailingTranscodeAndAssertFail(Job.State.THUMBNAILEXTRACTOR);
		
		VerifyOption verifyOption = new VerifyOption();
        verifyOption.transcoderNever = false;
        verifyOption.thumbnailExtractorNever = false;
        verifyOption.createdFileSenderNever = true;
        verifyOption.jobResultNotifierNever = true;

        verifyCollaboration(verifyOption);
	}
	
	@Test
	public void transcodingFailBecauseExceptionOccuredAtSendCreateFilesToDestination() {

		// void 타입일 경우 doThrow 사용 
		doThrow(mockException).when(createdFileSender).store(anyListOf(File.class), anyListOf(File.class), anyLong());
		assertJobIsWaitingState();
		excuteFailingTranscodeAndAssertFail(Job.State.CREATEDFILESENDOR);
		
		VerifyOption verifyOption = new VerifyOption();
        verifyOption.transcoderNever = false;
        verifyOption.thumbnailExtractorNever = false;
        verifyOption.createdFileSenderNever = false;
        verifyOption.jobResultNotifierNever = true;

        verifyCollaboration(verifyOption);
	}
	
	@Test
	public void transcodingFailBecauseExceptionOccuredAtNotifyJobResultToRequester() {
		
		doThrow(mockException).when(jobResultNotifier).notifyToRequester(anyLong());
		assertJobIsWaitingState();
		excuteFailingTranscodeAndAssertFail(Job.State.JOBRESULTNOTIFIER);
		
		VerifyOption verifyOption = new VerifyOption();
        verifyOption.transcoderNever = false;
        verifyOption.thumbnailExtractorNever = false;
        verifyOption.createdFileSenderNever = false;
        verifyOption.jobResultNotifierNever = false;

        verifyCollaboration(verifyOption);
	}
}
