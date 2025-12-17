
package schwalbe.ventura.engine

interface Disposable {

    fun dispose()

}

class UsageAfterDisposalException
    : RuntimeException("Usage of object after disposal")